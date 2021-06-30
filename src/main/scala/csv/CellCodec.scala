package csv

import StringCodecs.given

object CellCodec:
  
  given fromCodec[A](using s:StringCodec[A]):CellCodec[A] = s

  ///given  fromStringCodec[A]:CellCodec[A] = summon[StringCodec[A]] 
  given CellCodec[Double]  =  (StringCodec.from(s => StringResult(java.lang.Double.parseDouble(s.trim)))(_.toString))

  given CellCodec[Int]     =  StringCodec.from(s => StringResult(java.lang.Integer.parseInt(s.trim)))(_.toString)
  
  given CellCodec[Long]    =  StringCodec.from(s => StringResult(java.lang.Long.parseLong(s.trim)))(_.toString)

  given CellCodec[Boolean] =  StringCodec.from(s => StringResult(s.trim.toBoolean))(_.toString)

  given CellCodec[String]  =  StringCodec.from(s => Right(s))(_.toString)
  
  given  Option[String]  = Option("")

  //given fromCodecString[String](using s:StringCodec[String]):CellCodec[String] = s

  given  optSeq[A]:Option[Seq[A]]  =  Option(Seq.empty[A])

  given  optOption[A]: Option[Option[A]] = Option(Option.empty[A])

  given  optionCodec[A] (using CellCodec[A]):CellCodec[Option[A]] = Codec.withOptionalCodec

 

  

  