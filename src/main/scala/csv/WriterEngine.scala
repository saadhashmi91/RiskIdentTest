package csv
import java.io.Writer

trait WriterEngine:

  /** Creates a new instance of [[CSVWriter]] that writes encoded data to the specified writer.
    *
    * @param writer where to write encoded data.
    * @param conf column separator.
    */
   def writerFor(writer: Writer, conf: CSVConfiguration): CSVWriter[Seq[String],Unit]


/** Provides creation methods and default implementations. */
object WriterEngine:

  given internalCsvWriterEngine: WriterEngine = WriterEngine.from((w, c) => new CSVWriterImp(w, c))

  /** Creates a new instance of [[WriterEngine]] that wraps the specified function. */
  inline def from(f: (Writer, CSVConfiguration) => CSVWriter[Seq[String],Unit]): WriterEngine = new WriterEngine {
    override def writerFor(writer: Writer, conf: CSVConfiguration): CSVWriter[Seq[String],Unit] = f(writer, conf)
  }

  