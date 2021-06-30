package csv


object OpenResult extends ResultCompanion.WithError[ResourceError.OpenError]

object DecodeResult extends ResultCompanion.WithError[DecodeError]

object ParseResult extends ResultCompanion.WithDefault[ParseError]:
    override protected def fromThrowable(t: Throwable): ParseError = ParseError.IOError(t)


/** Provides useful methods for creating instances of [[ReadResult]]. */
object ReadResult extends ResultCompanion.Simple[ReadError]

object StringCodec extends CodecCompanion[String,DecodeError]

object StringResult extends ResultCompanion.WithError[DecodeError]

