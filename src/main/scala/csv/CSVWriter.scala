package csv

import java.io.{Closeable, Writer}
import scala.deriving.Mirror

/**
 * Type of values that know how to write CSV data.
 * @tparam I The input type parameter
 * @tparam O The output type parameter
 */
trait CSVWriter[I,O] extends Closeable:
  self =>

  def state: O

  def write(a: I): CSVWriter[I,O]
  
  def writeHeader(a: I): CSVWriter[I,O] 


  /** Releases the underlying resource.
    *
    * Calling this method when there is no more data to write is critical. Not doing so might result in a cached
    * resource not flushing its buffers, for example, and the resulting CSV data not being complete or even valid.
    */
  def close():Unit



/** Provides useful instance creation methods. */
object CSVWriter:
  /** Creates a new [[CSVWriter]] instance that will send encoded data to the specified `Writer`.
   * 
    * @param writer where to write CSV data to.
   * @param conf [[CSVConfiguration]]
   * @param engine [[WriterEngine]]
   * @param headerCodec [[HeaderCodec]]
   * @param m Mirror of A
   * @tparam A type of values that the returned instance will know to encode.
   */  
    transparent inline def apply[A](writer: Writer, conf: CSVConfiguration)(using engine: WriterEngine,headerCodec: HeaderCodec[A],m: Mirror.Of[A]) = {
      given  w:CSVWriter[Seq[String],Unit] = engine.writerFor(writer, conf)
      
      if conf.headers != null then      
        HeaderCodec.fromHeader[A](conf.headers) match
           case Right(codec) =>
              w.writeHeader(conf.headers)
             
              new CSVWriter[A,WriteResult[Unit]] {
                override def state = Right[WriteError,Unit](())
                override def write(a: A): CSVWriter[A,WriteResult[Unit]] = {w.write(codec.encode(a));this}
                override def writeHeader(a: A): CSVWriter[A,WriteResult[Unit]] = {w.writeHeader(codec.encode(a));this}
                override def close = w.close  
              }
           
           case Left(error)  =>
              new CSVWriter[A,WriteResult[Unit]] {
                override def state = Left[WriteError,Unit](WriteError(error.asInstanceOf[CodecError.MissingHeadersError].message))
                override def write(a: A): CSVWriter[A,WriteResult[Unit]] = this
                override def writeHeader(a: A): CSVWriter[A,WriteResult[Unit]] = this
                override def close = w.close    
              }

      else
        w.writeHeader(headerCodec.header.get)
        val codec = headerCodec.noHeader
         new CSVWriter[A,WriteResult[Unit]] {
                override def state = Right[WriteError,Unit](())
                override def write(a: A): CSVWriter[A,WriteResult[Unit]] = {w.write(codec.encode(a));this}
                override def writeHeader(a: A): CSVWriter[A,WriteResult[Unit]] = {w.writeHeader(codec.encode(a));this}
                override def close = w.close   
              }
       
    }

  /** Creates a new [[CSVWriter]] instance.
    *
    * This method is meant to help interface third party libraries with kantan.csv.
    *
    * @param out where to send CSV rows to - this is meant to be a third party library's csv writer.
    * @param w writes a CSV row using `out`.
    * @param r releases `out` once we're done writing.
    */
    transparent inline def apply[A](out: A)(w: (A, Seq[String]) => Unit)(r: A => Unit): CSVWriter[Seq[String],Unit] = new CSVWriter[Seq[String],Unit] {
      
      def state = Unit

      override def write(a: Seq[String]): CSVWriter[Seq[String],Unit] = {
        w(out, a)
        this
      }
      override def writeHeader(a: Seq[String]): CSVWriter[Seq[String],Unit] = {
        w(out, a)
        //Right(a)
        this
      }
      override def close(): Unit = r(out)
    }