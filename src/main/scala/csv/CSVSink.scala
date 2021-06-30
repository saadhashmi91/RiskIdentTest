package csv

import java.io.Writer
import scala.deriving.Mirror

/**
 * Type class for all types that can be turned into [[CsvWriter]] instances.
 * @see [[CSVSink companion object]] for default implementations and construction methods.
 * @tparam S Generic type modeling the sink to writer csv to
 */
trait CSVSink[-S]:
  self =>

    /** Opens a `Writer` on the specified `S`. */
  def open(s: S): Writer

  inline def writer[A](s: S, conf: CSVConfiguration)(using WriterEngine, HeaderCodec[A],Mirror.Of[A]) =
    CSVWriter(open(s), conf)

  def contramap[T](f: T => S): CSVSink[T] = CSVSink.from(f andThen self.open)
     
/** Provides default instances as well as instance summoning and creation methods. */
object CSVSink:

  given CSVSinkfromResource[A](using w:WriterResource[A]): CSVSink[A] =
    CSVSink.from(a =>
      w.open(a)
        .fold(
          error => sys.error(s"Failed to open resource $a: $error"),
          w => w
        )
    )

  /** Summons an implicit instance of `CSVSink[A]` if one can be found.
    *
    * This is simply a convenience method. The two following calls are equivalent:
    * {{{
    *   val file: CSVSink[File] = CsvSink[File]
    *   val file2: CSVSink[File] = summon[CsvSink[File]]
    * }}}
    */
  def apply[A](using CSVSink[A]): CSVSink[A] = summon[CSVSink[A]]

  /** Turns the specified function into a [[CSVSink]].
    *
    * Note that it's usually better to compose an existing instance through [[CSVSink.contramap]] rather than create
    * one from scratch.
    */
  def from[A](f: A => Writer): CSVSink[A] = new CSVSink[A] {
    override def open(s: A): Writer = f(s)
  }