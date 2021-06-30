package csv

import java.io.{Reader, StringReader, Writer}

trait Resource[I, R]:
  self =>

    /** Opens the specified resource. */
    def open(input: I): OpenResult[R]

    def contramap[II](f: II => I): Resource[II, R] = Resource.from(f andThen self.open)

/** Provides instance summoning methods for [[ReaderResource]]. */
object ReaderResource:

  /** Summons an implicit instance of [[ReaderResource]] if one is found in scope, fails compilation otherwise. */
  def apply[A](using ReaderResource[A]): ReaderResource[A] = summon[ReaderResource[A]]


/** Provides instance summoning methods for [[WriterResource]]. */
object WriterResource:

  /** Summons an implicit instance of [[WriterResource]] if one is found in scope, fails compilation otherwise. */
  def apply[A](using WriterResource[A]): WriterResource[A] = summon[WriterResource[A]]



object Resource:
  given [R <: Reader]: ReaderResource[R]    = Resource.from(Right.apply)
  given [W <: Writer]: WriterResource[W]    = Resource.from(Right.apply)
  given ReaderResource[String] = ReaderResource[Reader].contramap(s => new StringReader(s))

 
  def from[I, R](f: I => OpenResult[R]): Resource[I, R] = new Resource[I, R] {
    override def open(a: I) = f(a)
  }

  