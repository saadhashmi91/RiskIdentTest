package csv
import java.io.Reader
import com.univocity.parsers.csv._

/**
 * The ReaderEngine is used to either:
 * <ul>
 * <li>Create a custom [[CSVReader]] from a class that uses a [[java.io.Reader]] and [[CSVConfiguration]] to provide
 *   csv parsing functionality.</li>
 * <li>Summon an implicit [[CSVReader]].</li>
 * </ul>
 */
trait ReaderEngine:
    
     /** Turns the specified `Reader` into a [[CsvReader]]. */
  def unsafeReaderFor(reader: Reader,
                      conf: CSVConfiguration
                     ): CSVReader[Seq[String]]

  /** Turns the specified `Reader` into a safe [[CsvReader]]. */
  def readerFor(reader: => Reader,conf:CSVConfiguration): CSVReader[ReadResult[Seq[String]]] =
       val res = ParseResult(unsafeReaderFor(reader, conf))
                 .map(_.safe(ParseError.NoSuchElement:ParseError)( e =>  ParseError.IOError(e)))
                 .left
                 .map(e => ResourceIterator(ReadResult.failure(e)))
                 .merge
                 .withClose(() => reader.close())
                 
            res

object ReaderEngine:

  /** Default reader engine, used whenever a custom one is not explicitly brought in scope. */
  given internalCsvReaderEngine: ReaderEngine = ReaderEngine.from(BulkCsvReader.apply)


   /** Creates a new [[ReaderEngine]] instance. */
  def from(f: (Reader, CSVConfiguration) => CSVReader[Seq[String]]): ReaderEngine = new ReaderEngine {
    override def unsafeReaderFor(reader: Reader, conf: CSVConfiguration): CSVReader[Seq[String]] = f(reader, conf)
  }


/**
 * 
 * @param reader A [[java.io.Reader]] instance.
 * @param conf  A [[CSVConfiguration]] instance.
 */
private class  BulkCsvReader(reader: Reader,conf:CSVConfiguration) extends  CSVReader[Seq[String]]:
    
    lazy val parser: CsvParser = {
    val settings = new CsvParserSettings() 
    val format = settings.getFormat
    format.setDelimiter(conf.fieldSep)
    format.setLineSeparator(conf.lineSep)
    format.setQuote(conf.quote)
    format.setQuoteEscape(conf.escape)
    format.setComment(conf.commentMarker)
    settings.setIgnoreLeadingWhitespaces(conf.ignoreLeadingSpace)
    settings.setIgnoreTrailingWhitespaces(conf.ignoreTrailingSpace)
    settings.setReadInputOnSeparateThread(false)
    settings.setInputBufferSize(conf.inputBufSize)
    settings.setMaxColumns(conf.maxCols)
    settings.setNullValue("")
    settings.setMaxCharsPerColumn(conf.maxCharsPerCol)
    if (conf.headers != null) settings.setHeaders(conf.headers: _*)
    new CsvParser(settings)
    }

    parser.beginParsing(reader)

    private var nextRecord = parser.parseNext()

    override def next(): Seq[String] = 
      val curRecord = nextRecord
      if curRecord != null  then nextRecord = parser.parseNext()
      else throw new NoSuchElementException("next record is null")
      curRecord.toSeq
    

    override def hasNext: Boolean = nextRecord != null

    def close = 
     parser.stopParsing
     reader.close

private object BulkCsvReader:
  def apply(data: Reader, conf: CSVConfiguration): BulkCsvReader = new BulkCsvReader(data, conf)