package services.internal

import sttp.client3._
import zio._
import sttp.capabilities.Effect


type SttpClient = Has[SttpClient.Service]

object SttpClient:
   type Service = SttpBackend[Task, Any]
  

  /** Sends the request. Only requests for which the method & URI are specified can be sent.
    *
    * @return An effect resulting in a [[Response]], containing the body, deserialized as specified by the request
    *         (see [[RequestT.response]]), if the request was successful (1xx, 2xx, 3xx response codes), or if there
    *         was a protocol-level failure (4xx, 5xx response codes).
    *
    *         A failed effect, if an exception occurred when connecting to the target host, writing the request or
    *         reading the response.
    *
    *         Known exceptions are converted to one of [[SttpClientException]]. Other exceptions are kept unchanged.
    */
   def send[T](
      request: Request[T, Effect[Task]]
   ): ZIO[SttpClient, Throwable, Response[T]] =
    ZIO.accessM(env => env.get[SttpClient.Service].send(request))

