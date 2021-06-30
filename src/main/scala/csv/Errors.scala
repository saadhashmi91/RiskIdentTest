package csv


sealed abstract class CodecError(message:String) extends Error(message)

object CodecError {

  final case class MissingHeadersError(message: String) extends CodecError(message)

}

sealed abstract class ReadError(message: String) extends CodecError(message)
sealed case class WriteError(message: String) extends CodecError(message)

/** Parent type for all errors that can occur while parsing CSV data. */
sealed abstract class ParseError(message: String) extends ReadError(message)


/** Errors that can occur while working with a [[Resource]]. */
sealed abstract class ResourceError(message: String) extends Error(message)

object ResourceError {

  /** Errors that occur specifically while opening resources. */
  final case class OpenError(message: String) extends ResourceError(message)

  object OpenError extends ErrorCompanion("an unspecified error occurred while opening a resource")(new OpenError(_))

  /** Errors that occur specifically while processing resources. */
  final case class ProcessError(message: String) extends ResourceError(message)

  object ProcessError
      extends ErrorCompanion("an unspecified error occurred while processing a resource")(new ProcessError(_))

  /** Errors that occur specifically while closing resources. */
  final case class CloseError(message: String) extends ResourceError(message)

  object CloseError extends ErrorCompanion("an unspecified error occurred while closing a resource")(new CloseError(_))
}


/** Declares all possible values of type [[ParseError]]. */
object ParseError {

  /** Error that occurs when attempting to read from an empty [[CsvReader]]. */
  case object NoSuchElement extends ParseError("trying to read from an empty reader")

  /** Error that occurs while interacting with an IO resource.
    *
    * This is typically used to wrap a `java.io.IOException`.
    */
  final case class IOError(message: String) extends ParseError(message)

  /** Provides convenience methods for [[ParseError.IOError]] instance creation. */
  object IOError extends ErrorCompanion("an unspecified io error occurred")(s => new IOError(s))
}

sealed case class DecodeError(message: String) extends CodecError(message)

object DecodeError extends ErrorCompanion[DecodeError]("an error occurred while decoding data")(s => new DecodeError(s))
