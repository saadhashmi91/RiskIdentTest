package services
import sttp.client3._
import zio._
import csv._

import csv.CaseClassCodec.sequence
//import csv.CSVExtensions2.CSVExtensions2
import ArticlesService.ArticlesService
import services.internal.SttpClient

// id|productId|name|description |price|stock
// id|produktId|name|beschreibung|preis|bestand (String|String|String|String|Float|Int)
final case class Articles(id:String,produktId:String,name:String,beschreibung:String,preis:Float,bestand:Int)

/**
 * Main service to interact with remote csv provisioning and validation service.
 */
object ArticlesService:

  type ArticlesService = Has[Service]

   
  private val csvConf = CSVConfiguration(fieldSep = '|',headers = Seq("id","produktId","name","beschreibung","preis","bestand"))
  private val csvReader = (s:String) => CSVReaderWriter.asCSVReader[String,Articles](s,csvConf)
    
  private val asCSV: ResponseAs[Iterator[Either[ReadError,Articles]], Any] = asStringAlways.map( s => csvReader(s))


  trait Service {
    def getArticles(lines:Int): Task[Either[ReadError,Iterator[Articles]]]
    def putArticles(articles:String,lines:Int):Task[Either[String,String]]
  }

  val live: ZLayer[SttpClient, Throwable , ArticlesService] =
    ZLayer.fromService { sttpClient =>
      new Service {
        def getArticles(lines:Int): Task[Either[ReadError,Iterator[Articles]]] = {
          val request = basicRequest
          .get(uri"http://localhost:8080/articles/$lines")
          .response(asCSV)
          sttpClient.send(request).map(_.body.sequence)
        }
       
        def putArticles(articles:String,lines:Int):Task[Either[String,String]] = {
          val request  = basicRequest
                         .body(articles)
                         .put(uri"http://localhost:8080/products/$lines")
              sttpClient.send(request).map(_.body)
        }
      
      }}

  /**
   * Accessor method
   * @param lines The number of lines to read from the remote service.
   * @return [[Iterator]] of [[Articles]]
   */
  def getArticlesAccessor(lines:Int): ZIO[ArticlesService, Throwable, Iterator[Articles]] =
     ZIO.accessM[ArticlesService]{ effect => effect.get.getArticles(lines)}.absolve

  /**
   * Accesor method
   * @param articles The encoded csv data to send to the remote service
   * @param lines The number of underlying articles from which the result is derived.
   * @return Returns the validation result
   */
  def putArticlesAccessor(articles:String,lines:Int): ZIO[ArticlesService, Throwable, String] =
     ZIO.accessM[ArticlesService]{_.get.putArticles(articles,lines).map { x => x.left.map(WriteError(_))}}.absolve

   
  
  
