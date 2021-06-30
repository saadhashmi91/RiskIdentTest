package csv
import scala.deriving.Mirror
import java.io.Writer

/**
 * CSVExtensions provides conveniant extension methods to provide:
 * <ul>
 *   <li> Functionality to read csv from a string representation of csv.</li>
 *   <li> Functionality to write domain objects to a [[java.io.Writer]].</li>
 *   </ul>
 */
trait CSVExtensions:    
  
 
   
   extension[A,B](a: A)
   
   
          /*
           * {{{
           * scala> import csv._
           *
           * scala> case class Decoded(i1:Int,i2:Int,i3:Int)
           * ...
           * scala> given codec:RowCodec[Decoded] = CaseClassCodec.derived[Decoded]
           * ...
           * scala> "1,2,3\n4,5,6".asCSVReader[Decoded](CSVConfiguration()).toList
           * res0: List[ReadResult[Decoded]] = List(Right(Decoded(1, 2, 3)), Right(Decoded(4, 5, 6)))
           * }}}
           *
           */
         inline def asCSVReader(conf: CSVConfiguration)(using  ReaderEngine,CSVSource[A],Mirror.Of[B]): CSVReader[ReadResult[B]] =
           summon[CSVSource[A]].reader[B](a, conf)
           
         inline def asCSVWriter(conf: CSVConfiguration)(using WriterEngine, HeaderCodec[B],CSVSink[A],Mirror.Of[B]) =
           summon[CSVSink[A]].writer(a, conf)
         
