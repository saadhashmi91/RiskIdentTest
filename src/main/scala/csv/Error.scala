package csv

abstract class Error(message: String) extends Exception(message) with Product with Serializable {
  override final def toString: String = productPrefix + ": " + getMessage
}

abstract class ErrorCompanion[T <: Error](defaultMsg: String)(f: String => T) extends Serializable {
  given isError: IsError[T] = new IsError[T] {
    override def from(msg: String, cause: Throwable) = {
      val error = f(msg)
      error.initCause(cause)
      error
    }

    override def fromMessage(msg: String) = from(msg, new Exception(msg))

    override def fromThrowable(cause: Throwable): T = from(Option(cause.getMessage).getOrElse(defaultMsg), cause)
  }

  /** Attempts to evaluate the specified argument, wrapping errors in a `T`. */
  def safe[A](a: => A): Either[T, A] = isError.safe(a)

  def apply(msg: String, cause: Throwable): T = isError.from(msg, cause)
  def apply(cause: Throwable): T              = isError.fromThrowable(cause)
  def apply(msg: String): T                   = isError.fromMessage(msg)
}