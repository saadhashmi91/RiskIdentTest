package csv

import java.io.Reader
import scala.deriving.Mirror

/**
 * CSVReader uses implicit [[HeaderCodec]] and [[ReaderEngine]] instances to decode csv data into a generic
 * ADT modeling the csv columns. The csv data is read from a [[java.io.Reader]] instance.
 */
object CSVReader:
 inline def apply[A](reader: Reader, conf: CSVConfiguration)(
      using e: ReaderEngine, m:Mirror.Of[A],h:HeaderCodec[A]): CSVReader[ReadResult[A]] = 
       
    val data: CSVReader[ReadResult[Seq[String]]] = e.readerFor(reader, conf)
        
    val codec =
      if conf.headers != null && data.hasNext then
        for
           header  <- data.next()
           codec   <- HeaderCodec.fromHeader(header.map(_.trim))
        yield codec      
      else Right[CodecError,RowCodec[A]](h.noHeader)
    
    codec
      .map(d => data.map(_.flatMap( a => d.decode(a))))
      .left
      .map(error => ResourceIterator(ReadResult.failure(error.asInstanceOf[ReadError])))
      .merge.asInstanceOf[CSVReader[ReadResult[A]]]
        