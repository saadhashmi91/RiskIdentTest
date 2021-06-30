import java.io.{Reader, Writer,StringReader}
import scala.deriving.Mirror


package object csv extends CSVExtensions with ResourceIteratorExtension {


type StringCodec[A] = Codec[String, A, DecodeError]

type CellCodec[A] = Codec[String, A, DecodeError]

type OpenResult[A]     = Either[ResourceError.OpenError, A]
type CloseResult       = Either[ResourceError.CloseError, Unit]
type ProcessResult[A]  = Either[ResourceError.ProcessError, A]
type ResourceResult[A] = Either[ResourceError, A]

type ReaderResource[A] = Resource[A, Reader]
type WriterResource[A] = Resource[A, Writer]

type ParseResult[A] = Either[ParseError, A]

type ReadResult[A] = Either[ReadError, A]

type WriteResult[A] = Either[WriteError,A]

type CodecResult[A] = Either[CodecError,A]

type DecodeResult[A] = Either[DecodeError, A]

type RowCodec[A] = Codec[Seq[String], A, DecodeError]

type Factory[A, C] = scala.collection.Factory[A, C]

type CSVReader[A] = Iterator[A]

type StringResult[A] = Either[DecodeError, A]



// Moved From Resource Companion Object

// Moved From CaseClassCodec Companion Object


// Moved From HeaderCodec Companion Object


// Moved From CSVSource Companion Object

// Moved From CSVSink Companion Object



// Moved From ReaderEngine Companion Object    



// Moved From WirterEngine Companion Object



}

