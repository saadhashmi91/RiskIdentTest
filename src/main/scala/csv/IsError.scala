package csv
/** Type class for types that can be used as errors.
  *
  * This is mostly meant to work in conjunction with [[Error]], and lets code that deals with errors turn them into
  * values of the right error type.
  */
trait IsError[E] extends Serializable:
  self =>

  /** Creates a new instance of `E` from an exception. */
  def fromThrowable(t: Throwable): E

  /** Creates a new instance of `E` from an error message. */
  def fromMessage(msg: String): E

  /** Creates a new instance of `E` from an error message and exception. */
  def from(msg: String, t: Throwable): E

  /** Safely evaluates the specified argument, wrapping errors in a `E`. */
  def safe[A](a: => A): Either[E, A] = ResultCompanion.nonFatal(fromThrowable)(a)



object IsError:

  /** Summons an implicit instance of `IsError[A]` if one is found in scope, fails compilation otherwise. */
  def apply[A](using ev: IsError[A]): IsError[A] = summon[IsError[A]]

  /** Default instance for `Exception.` */
  given IsError[Exception] = new IsError[Exception] {
    override def fromThrowable(t: Throwable)     = new Exception(t)
    override def fromMessage(msg: String)        = new Exception(msg)
    override def from(msg: String, t: Throwable) = new Exception(msg, t)
  }
