package csv

import java.io.{IOException, Reader}
import scala.deriving.Mirror

/**
 *  Turns instances of `S` into valid sources of CSV data.
 * @tparam S The generic type modeling the csv source to read from
 */
trait CSVSource[-S]:
   self =>
     
    def open(s: S): ParseResult[Reader]

    inline def reader[A](s: S, conf: CSVConfiguration)(using ReaderEngine,Mirror.Of[A]): CSVReader[ReadResult[A]] =
      open(s)
      .map(reader => CSVReader(reader, conf))
      .left
      .map(error => ResourceIterator(ReadResult.failure(error)))
      .merge

object CSVSource:

  given CSVSourcefromResource[A](using r: ReaderResource[A]): CSVSource[A] =
    CSVSource.from(a => r.open(a.asInstanceOf[A]).left.map(e => ParseError.IOError(e.getMessage, e.getCause)))


  /** Summons an implicit instance of `CSVSource[A]` if one can be found.
    *
    * This is basically a less verbose, slightly faster version of `implicitly`.
    */
  def apply[A](using CSVSource[A]): CSVSource[A] = summon[CSVSource[A]]

  /** Turns the specified function into a [[CSVSource]].
    */
  def from[A](f: A => ParseResult[Reader]): CSVSource[A] = new CSVSource[A] {
    override def open(a: A): ParseResult[Reader] = f(a)
  }