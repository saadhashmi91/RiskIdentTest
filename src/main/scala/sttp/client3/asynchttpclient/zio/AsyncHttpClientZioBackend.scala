package sttp.client3.asynchttpclient.zio

import services.internal._

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream}
import java.nio.ByteBuffer
import zio._
import zio.blocking.Blocking
import zio.interop.reactivestreams.{
  publisherToStream => publisherToZioStream,
  streamToPublisher => zioStreamToPublisher
}
import zio.stream._
import io.netty.buffer.{ByteBuf, Unpooled}
import org.asynchttpclient.{
  AsyncHttpClient,
  AsyncHttpClientConfig,
  BoundRequestBuilder,
  DefaultAsyncHttpClient,
  DefaultAsyncHttpClientConfig,
  AsyncHandler,
  HttpResponseBodyPart,
  HttpResponseStatus,
  Realm,
  RequestBuilder,
  Request => AsyncRequest,
  Response => AsyncResponse
}
import scala.collection.JavaConverters._
import sttp.client3.SttpBackendOptions.ProxyType.{Http, Socks}
import org.asynchttpclient.proxy.ProxyServer
import org.reactivestreams.Publisher
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.asynchttpclient.{AsyncHttpClientBackend, BodyFromAHC, BodyToAHC}
import sttp.client3.impl.zio.{RIOMonadAsyncError, ZioSimpleQueue, ZioWebSockets}
import sttp.client3.internal._
import sttp.client3.internal.ws.SimpleQueue
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{FollowRedirectsBackend, SttpBackend, SttpBackendOptions}
import sttp.monad.MonadAsyncError
import sttp.ws.{WebSocket, WebSocketFrame}


class AsyncHttpClientZioBackend private (
    runtime: Runtime[Any],
    asyncHttpClient: AsyncHttpClient,
    closeClient: Boolean,
    customizeRequest: BoundRequestBuilder => BoundRequestBuilder,
    webSocketBufferCapacity: Option[Int]
) extends AsyncHttpClientBackend[Task, ZioStreams, ZioStreams with WebSockets](
      asyncHttpClient,
      new RIOMonadAsyncError[Any],
      closeClient,
      customizeRequest
    ){

   override val streams: ZioStreams = ZioStreams

   private val bufferSize = 16

   override protected val bodyFromAHC: BodyFromAHC[Task, ZioStreams] = 
     new BodyFromAHC[Task, ZioStreams] {
      override val streams: ZioStreams = ZioStreams
      override implicit val monad: MonadAsyncError[Task] = new RIOMonadAsyncError[Any]
 
      override def publisherToStream(p: Publisher[ByteBuffer]): Stream[Throwable, Byte] =
         p.toStream(bufferSize).mapConcatChunk(Chunk.fromByteBuffer(_))
 
      override def publisherToBytes(p: Publisher[ByteBuffer]): Task[Array[Byte]] =
         p.toStream(bufferSize).fold(ByteBuffer.allocate(0))(concatByteBuffers).map(_.array())
 
      override def publisherToFile(p: Publisher[ByteBuffer], f: File): Task[Unit] = {
         p.toStream(bufferSize)
           .map(Chunk.fromByteBuffer)
           .flattenChunks
           .run(ZSink.fromOutputStream(new FileOutputStream(f)))
           .unit
           .provideLayer(Blocking.live)
       }
    

      override def bytesToPublisher(b: Array[Byte]): Task[Publisher[ByteBuffer]] =
        Stream.apply(ByteBuffer.wrap(b)).toPublisher

      override def fileToPublisher(f: File): Task[Publisher[ByteBuffer]] =
        ZStream
          .fromInputStream(new BufferedInputStream(new FileInputStream(f)))
          .mapChunks(ch => Chunk(ByteBuffer.wrap(ch.toArray)))
          .toPublisher
          .provideLayer(Blocking.live)

      override def compileWebSocketPipe(
          ws: WebSocket[Task],
          pipe: Stream[Throwable, WebSocketFrame.Data[_]] => Stream[Throwable, WebSocketFrame]
      ): Task[Unit] = ZioWebSockets.compilePipe(ws, pipe)
    }

   override protected val bodyToAHC: BodyToAHC[Task, ZioStreams] = new BodyToAHC[Task, ZioStreams] {
    override val streams: ZioStreams = ZioStreams

    override protected def streamToPublisher(s: Stream[Throwable, Byte]): Publisher[ByteBuf] =
      runtime.unsafeRun(s.mapChunks(c => Chunk.single(Unpooled.wrappedBuffer(c.toArray))).toPublisher)
  }

   override protected def createSimpleQueue[T]: Task[SimpleQueue[Task, T]] =
    for {
      runtime <- ZIO.runtime[Any]
      queue <- webSocketBufferCapacity match {
        case Some(capacity) => Queue.dropping[T](capacity)
        case None           => Queue.unbounded[T]
      }
    } yield new ZioSimpleQueue(queue, runtime)
  }

object AsyncHttpClientZioBackend {
  private def apply[R](
      runtime: Runtime[R],
      asyncHttpClient: AsyncHttpClient,
      closeClient: Boolean,
      customizeRequest: BoundRequestBuilder => BoundRequestBuilder,
      webSocketBufferCapacity: Option[Int]
  ): SttpBackend[Task, ZioStreams with WebSockets] =
    new FollowRedirectsBackend(
      new AsyncHttpClientZioBackend(runtime, asyncHttpClient, closeClient, customizeRequest, webSocketBufferCapacity)
    )

  def apply(
      options: SttpBackendOptions = SttpBackendOptions.Default,
      customizeRequest: BoundRequestBuilder => BoundRequestBuilder = identity,
      webSocketBufferCapacity: Option[Int] = AsyncHttpClientBackend.DefaultWebSocketBufferCapacity
  ): Task[SttpBackend[Task, ZioStreams with WebSockets]] =
    ZIO
      .runtime[Any]
      .flatMap(runtime =>
        Task.effect(
          AsyncHttpClientZioBackend(
            runtime,
            AsyncHttpClientBackend.defaultClient(options),
            closeClient = true,
            customizeRequest,
            webSocketBufferCapacity
          )
        )
      )

  def managed(
      options: SttpBackendOptions = SttpBackendOptions.Default,
      customizeRequest: BoundRequestBuilder => BoundRequestBuilder = identity,
      webSocketBufferCapacity: Option[Int] = AsyncHttpClientBackend.DefaultWebSocketBufferCapacity
  ): TaskManaged[SttpBackend[Task, ZioStreams with WebSockets]] =
    ZManaged.make(apply(options, customizeRequest, webSocketBufferCapacity))(_.close().ignore)

  def layer(
      options: SttpBackendOptions = SttpBackendOptions.Default,
      customizeRequest: BoundRequestBuilder => BoundRequestBuilder = identity,
      webSocketBufferCapacity: Option[Int] = AsyncHttpClientBackend.DefaultWebSocketBufferCapacity
  ): Layer[Throwable, SttpClient] =
    ZLayer.fromManaged(managed(options, customizeRequest, webSocketBufferCapacity))

 
}