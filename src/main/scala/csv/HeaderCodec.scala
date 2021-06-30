package csv

import scala.deriving.Mirror
import CaseClassCodec.{_}

/** Provides instance summoning and creation methods for [[HeaderCodec]]. */
trait HeaderCodec[A]:
   
   def noHeader: RowCodec[A]
   def header: Option[Seq[String]]

object HeaderCodec :
  
  /** Summons an implicit instance of [[HeaderCodec]] if one can be found, fails compilation otherwise. */
  inline given defaultHeaderCodec[A](using m: Mirror.Of[A]): HeaderCodec[A] = new HeaderCodec[A] {
    override def noHeader                          = CaseClassCodec.derived[A]
    override def header:Option[Seq[String]]        = Some(CaseClassCodec.getElemLabels[m.MirroredElemLabels]) 
  }

  def apply[A](using h:HeaderCodec[A]): HeaderCodec[A] = h

  inline def fromHeader[A](header: Seq[String])(using Mirror.Of[A]): CodecResult[RowCodec[A]] = codecFromHeader[A](headers = header)

