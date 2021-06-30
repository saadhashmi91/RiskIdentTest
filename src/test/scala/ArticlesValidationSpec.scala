import org.scalatest.flatspec.AnyFlatSpec
import zio.Exit.{Failure, Success}
import zio.{BootstrapRuntime, ZIO}

class ArticlesValidationSpec  extends AnyFlatSpec {


  val testValues = Seq()

  "Full application" should "give HTTP 200 OK with live implementation of Articles service" in {

      val runtimeTest = TestApp.makeRuntime

      runtimeTest.unsafeRunSync(TestApp.runTest("--lines" :: "100" :: Nil)) match {
        case Success(value) =>
          //println(s"Read: $value")
          assertResult("0")(value)
        case Failure(cause) => fail(cause.prettyPrint)
      }

      runtimeTest.unsafeRunSync(TestApp.runTest("--lines" :: "1000" :: Nil)) match {
        case Success(value) =>
          //println(s"Read: $value")
          assertResult("0")(value)
        case Failure(cause) => fail(cause.prettyPrint)
      }

    }
  

}

object TestApp extends Application {
  def runTest(args: List[String]): ZIO[zio.ZEnv, Throwable, String] = {
    super.run(args)
  }

  lazy final override val makeRuntime: BootstrapRuntime = super.makeRuntime
}
