package csv
import scala.deriving.Mirror

object CSVReaderWriter {


    inline def asCSVReader[A,B](a:A,conf: CSVConfiguration)(using  ReaderEngine,CSVSource[A],Mirror.Of[B]): CSVReader[ReadResult[B]] =
        summon[CSVSource[A]].reader[B](a, conf)

    
    inline def asCSVWriter[A,B](a:A,conf: CSVConfiguration)(using WriterEngine, HeaderCodec[B],CSVSink[A],Mirror.Of[B]) =
        summon[CSVSink[A]].writer(a, conf)

   


}