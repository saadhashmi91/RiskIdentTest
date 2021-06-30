import Application.APP_ENV
import services.internal.{Logger,ZioApplication}
import services.{Arguments, Log,ArticlesService,Articles}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import izumi.reflect.Tag
import zio.{Task, UIO, ZIO, ZLayer,App}
import zio.console.putStrLn
import csv._
import java.io.StringWriter
import scala.collection.mutable.LinkedHashMap
import scala.annotation.tailrec

trait Application extends ZioApplication[Arguments, APP_ENV, String] {


  implicit lazy final override val tagC: Tag[Arguments] = Tag.tagFromTagMacro
  implicit lazy final override val tagEnv: Tag[APP_ENV] = Tag.tagFromTagMacro

  lazy final override protected val env: ZLayer[ZIO_ENV, Throwable, APP_ENV] =
    (AsyncHttpClientZioBackend.layer() >>> ArticlesService.live) 

  override protected def makeCli(args: List[String]): Arguments = Arguments(args)
  lazy final override protected val loggerFactory: FACTORY_LOG = Logger.Factory(services.Log.apply)
 // productId|name|description |price|sum of stocks
 // produktId|name|beschreibung|preis|summeBestand (String|String|String|Float|Int)

  final case class ToArticles(produktId:String,name:String,beschreibung:String,preis:String,summeBestand:Int)    
  
  extension[A,K](l: Traversable[A])

    def inOrderGroupBy(f: A => K): Map[K,Traversable[A]] = 
     {  val grouped = l.zipWithIndex.groupBy[K](x => f(x._1)).toSeq
        val lhm     = LinkedHashMap(grouped.sortBy (_._2.head._2): _*)
        val out     = lhm.view.mapValues (_ map (_._1)).toMap
            out
    }
    def orderedGroupBy(f: A => K): Seq[(K, Traversable[A])] = {
        @tailrec
        def accumulator(seq: Traversable[A], f: A => K, res: List[(K, Traversable[A])]): Seq[(K, Traversable[A])] = seq.headOption match {
          case None => res.reverse
          case Some(h) => {
            val key = f(h)
            val subseq = seq.takeWhile(f(_) == key)
            accumulator(seq.drop(subseq.size), f, (key -> subseq) :: res)
          }}
        accumulator(l, f, Nil)
      }


  def doTransformation(articles:Iterator[Articles]):Iterable[ToArticles] = 
  {
     val articlesList  = articles.toList.filter(_.bestand > 0)

     val results = articlesList.orderedGroupBy(_.produktId.trim).map{
       case(key,values) => 
           val totalStock      = values.map(_.bestand).fold(0)(_ + _)
           val minPrice        = values.map(_.preis).min
           val cheapestArticle = values.minBy(_.preis)
           val productId       = cheapestArticle.produktId
           val name            = cheapestArticle.name
           val description     = cheapestArticle.beschreibung
           ToArticles(productId,name,description,f"${minPrice}%1.2f",totalStock)
        }               
          
        //print(articlesStream) 
        results.toList
            
  }

  override def runApp(): ZIO[COMPLETE_ENV, Throwable, String] = {
   val result = for 
      linesOut     <- Arguments(_.lines)
      data         <- ArticlesService.getArticlesAccessor(linesOut())
      transformed  =  doTransformation(data)
      stringWriter =  new StringWriter
      csvWriter    =  CSVReaderWriter.asCSVWriter[StringWriter,ToArticles](stringWriter,CSVConfiguration(fieldSep = '|'))
      string       <- UIO(transformed.foreach(csvWriter.write(_))) *> UIO(csvWriter.close) *> UIO(stringWriter.toString)
      code         <- ArticlesService.putArticlesAccessor(string,linesOut())
      _            <- putStrLn(code)
    yield code
    result
 
}

}

object Application  {

  type APP_ENV = ArticlesService.ArticlesService

}
object AppMain extends Application {
  def runMain(args: List[String]): ZIO[zio.ZEnv, Throwable, String] = {
    super.run(args)
  }
}