package csv

import java.io.Writer
import com.univocity.parsers.csv._

/**
 * Provides default implementation of writing csv data.
 * @param out Instance of [[java.io.Writer]]
 * @param conf Instance of [[CSVConfiguration]]
 */
private class CSVWriterImp (private val out: Writer, val conf: CSVConfiguration) extends CSVWriter[Seq[String],Unit]:
    
    def state = ()

    lazy val writer: CsvWriter = {
      val settings = new CsvWriterSettings() 
      val format = settings.getFormat
      format.setDelimiter(conf.fieldSep)
      format.setLineSeparator(conf.lineSep)
      format.setQuote(conf.quote)
      format.setQuoteEscape(conf.escape)
      format.setComment(conf.commentMarker)
      settings.setIgnoreLeadingWhitespaces(conf.ignoreLeadingSpace)
      settings.setIgnoreTrailingWhitespaces(conf.ignoreTrailingSpace)
      settings.setMaxColumns(conf.maxCols)
      settings.setNullValue("")
      settings.setMaxCharsPerColumn(conf.maxCharsPerCol)
      if conf.headers != null then settings.setHeaders(conf.headers: _*)
      new CsvWriter(out, settings);
    }   
   
    override def write(ss: Seq[String]): CSVWriter[Seq[String],Unit] = {
     writer.writeRow(ss.toArray)
     //Right(ss)
     this
     }

    override def writeHeader(ss:Seq[String]): CSVWriter[Seq[String],Unit] = 
      writer.writeHeaders(ss:_*)
      Right(ss)
      this

    override def close(): Unit = 
     writer.close()
     out.close()