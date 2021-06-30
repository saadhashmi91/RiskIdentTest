package csv

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline,summonFrom}
import scala.collection.BuildFrom
import scala.collection.mutable


/**
 * CaseClassCodec provides the main functionality to derive [[RowCodec]] using typeclass derivation as 
 * explained [[https://dotty.epfl.ch/docs/reference/contextual/derivation.html here]].
 **/
object CaseClassCodec:

  inline def getElemLabels[A <: Tuple]: List[String] =
     inline erasedValue[A] match
       case _: EmptyTuple => Nil // stop condition - the tuple is empty
       case _: (head *: tail) =>  // yes, in scala 3 we can match on tuples head and tail to deconstruct them step by step
         constValue[head].toString :: getElemLabels[tail]
   
  private inline def summonCellCodecsAll[T <: Tuple]: List[CellCodec[Any]] = inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      // summonInline[StringCodec[t]]
      case _: (t *: ts) => summonInline[CellCodec[t]].asInstanceOf[CellCodec[Any]] :: summonCellCodecsAll[ts]
    }

 
  private inline def toTuple[Xs <: Tuple](x:List[Any],index:Int) : Tuple = inline erasedValue[Xs] match
       case t: (head *: tail)  => x(index) *: toTuple[tail](x,index + 1) 
       case _ => EmptyTuple
    
   /** Turns a collection of results into a result of a collection. */
  extension[S,M[X] <: IterableOnce[X],F](rs: M[Either[F, S]]){ 
    inline def sequence(
      using bf: BuildFrom[M[Either[F, S]], S, M[S]]
    ): Either[F, M[S]] =
      rs.iterator
        .foldLeft(Right(bf.newBuilder(rs)): Either[F, mutable.Builder[S, M[S]]]) { (builder, res) =>
          for {
            b <- builder
            r <- res
          } yield b += r
        }
        .map(_.result())
    }
   
  inline def decodeChildern[T <: Tuple](elemLabels:Seq[String],row:Seq[String],mapping:Map[String,Int]): Tuple =
     inline erasedValue[T] match {
      case b: (head *: tail) =>
             val elem:String = row(mapping(elemLabels.head))
             val decoded =
               summonFrom {
                 case codec: CellCodec[head] => codec.decode(elem)
                          }
             ( decoded *: decodeChildern[tail](elemLabels.tail,row,mapping))
      case _ => EmptyTuple
     }
   
  inline def deriveCaseClassCodec[A](using m: Mirror.ProductOf[A],mapping:Option[Seq[String]]):RowCodec[A] =
     val elemLabels = getElemLabels[m.MirroredElemLabels]         
     val codecs = summonCellCodecsAll[m.MirroredElemTypes]

     new RowCodec[A] {
          

         def encode(d: A): Seq[String] = 
            val elems      = d.asInstanceOf[Product].productIterator.toList
            val zipped     = elemLabels.zip(elems).zip(codecs.toList)
            val encoded    = zipped.map{ case ((elemLabel,elem),codec) => elemLabel -> codec.encode(elem) }.toMap
            val mapped     = mapping.getOrElse(elemLabels).map( s => encoded(s))    
            mapped
            
         def decode(elems: Seq[String]): Either[DecodeError,A] =
            val mapped  = mapping.getOrElse(elemLabels).zip(elems).map { case(colName,elem) => colName -> elem }.toMap
            val zipped  = elemLabels.zip(codecs)
            val decoded = zipped.map{ case(elemLabel,codec) => codec.decode(mapped(elemLabel)) }               
            val out = for elems <- decoded.sequence
                          tuple = toTuple[m.MirroredElemLabels](elems,0)                
                      yield tuple

            out.map( t => m.fromProduct(t.asInstanceOf[Product]).asInstanceOf[A])
                
          
       }
  
  
  inline given derived[A](using m: Mirror.Of[A]): RowCodec[A] =
    inline m match 
      case s: Mirror.SumOf[A]     => ???
      case p: Mirror.ProductOf[A] => CaseClassCodec.deriveCaseClassCodec(using p,None)

  inline def codecFromHeader[A](headers:Seq[String])(using m:Mirror.Of[A]): CodecResult[RowCodec[A]] = 
    inline m match 
      case s: Mirror.SumOf[A]     => ???
      case p: Mirror.ProductOf[A] => 
        val elemLabelsSet = getElemLabels[m.MirroredElemLabels].toSet
        val headersSet = headers.toSet
        val setDiff    = headersSet &~ elemLabelsSet
        if  setDiff != Set.empty[String] && headersSet.size > elemLabelsSet.size
        then Left(CodecError.MissingHeadersError( "Missing Header(s): " + setDiff.toSeq.mkString(", ") + "."))
        else Right(deriveCaseClassCodec(using p,Some(headers)))
