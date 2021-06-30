package csv

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline,summonFrom}
import scala.collection.BuildFrom
import scala.collection.mutable

/**
 * Codec provides the two main CSV codec behaviours, '''encode''' and '''decode'''.
 * @tparam E The encoded type.
 * @tparam D The decoded type.
 * @tparam F The error type.
 */
trait Codec[E,D,F]:
  def encode(d:D): E
  def decode(e:E): Either[F,D]
  


object Codec:


  inline def from[E, D, F](f: E =>  Either[F,D])(g: D => E): Codec[E, D,F] = 
    new Codec[E, D, F] {
      def encode(d: D) = g(d)

      def decode(e: E) = f(e)

    }
  
  given withOptionalCodec[E, D, F](using codec: Codec[E, D, F], option:Option[E]): Codec[E, Option[D], F] =
    Codec.from( (e:E) =>
      if Option[E](e).isEmpty then Right(None)
      else  codec.decode(e).map(Some[D](_))
    )( d =>  d.map(codec.encode).getOrElse{
        val empty = summon[Option[E]].empty
        empty.head
         })
  
  given CellCodec[Double]  =  (StringCodec.from(s => StringResult(java.lang.Double.parseDouble(s.trim)))(_.toString))
  
  given CellCodec[Float]   =  StringCodec.from(s => StringResult(java.lang.Float.parseFloat(s.trim)))(_.toString)

  given CellCodec[Int]     =  StringCodec.from(s => StringResult(java.lang.Integer.parseInt(s.trim)))(_.toString)
  
  given CellCodec[Long]    =  StringCodec.from(s => StringResult(java.lang.Long.parseLong(s.trim)))(_.toString)

  given CellCodec[Boolean] =  StringCodec.from(s => StringResult(s.trim.toBoolean))(_.toString)

  given CellCodec[String]  =  StringCodec.from(s => Right(s))(_.toString) 
   

 
trait CodecCompanion[E,F]:
   inline def from[D](f: E => Either[F,D])(g: D => E): Codec[E, D,F]  = Codec.from(f)(g)

   

