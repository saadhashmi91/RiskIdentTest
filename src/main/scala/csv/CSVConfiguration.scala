package csv

/**
 * The CSVConfiguration class provides configuration options for CSV parsing and writing.
 * @param fieldSep Csv column separator
 * @param lineSep  Csv line separator 
 * @param quote  Csv quote character
 * @param escape Csv escape chracter
 * @param commentMarker Csv comment marker
 * @param ignoreLeadingSpace Whether to ignore leading space
 * @param ignoreTrailingSpace Whether to ignore trailing space
 * @param headers Csv headers
 * @param inputBufSize Csv input buffer size
 * @param maxCols Csv maximum number of columns
 * @param maxCharsPerCol Csv maximum characters per column
 */
final case class CSVConfiguration(
                      fieldSep: Char = ',',
                      lineSep: String = "\n",
                      quote: Char = '"',
                      escape: Char = '\\',
                      commentMarker: Char = '#',
                      ignoreLeadingSpace: Boolean = true,
                      ignoreTrailingSpace: Boolean = true,
                      headers: Seq[String] = null,
                      inputBufSize: Int = 128,
                      maxCols: Int = 20480,
                      maxCharsPerCol: Int = 100000)
