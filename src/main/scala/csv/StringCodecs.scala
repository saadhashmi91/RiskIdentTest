package csv

/**
 * Useful typeclass instances of csv [[Codec]] for encoding and decoding primitive data types.
 */
object StringCodecs:
  given StringCodec[Double]  =  (StringCodec.from(s => StringResult(java.lang.Double.parseDouble(s.trim)))(_.toString))

  given StringCodec[Int]     =  StringCodec.from(s => StringResult(java.lang.Integer.parseInt(s.trim)))(_.toString)
  
  given StringCodec[Long]    =  StringCodec.from(s => StringResult(java.lang.Long.parseLong(s.trim)))(_.toString)

  given StringCodec[Boolean] =  StringCodec.from(s => StringResult(s.trim.toBoolean))(_.toString)

  given StringCodec[String]  =  StringCodec.from(s => Right(s))(_.toString)

  

  

  





           
          