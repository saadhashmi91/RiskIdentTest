
package services.internal
import services.Arguments
import services.internal.{CommandLineArguments => CLA}
import services.internal.Logger.Logger
import services.internal.CommandLineArguments.Helper.HelpHandlerException
import zio.{BootstrapRuntime,Task,App,Tag,ZEnv,Has, UIO, ZIO, ZLayer}
import zio.internal.Platform
import ZioApp.BaseEnv
import csv._

trait ZioApplication[C <: CLA.Service, ENV <: Has[_], OUTPUT]:

  // Shortcut types
  final protected type COMPLETE_ENV = ENV with ZioApp.ZIOEnv[C]
  final protected type ZIO_ENV = ZioApp.ZIOEnv[C]
  final protected type FACTORY_LOG = Logger.Factory
  final protected type FACTORY_CLI = CLA.Factory[C]

  // Tag for user env
  implicit def tagC:   Tag[C]
  implicit def tagEnv: Tag[ENV]


  // Build ZIO environment
  protected def loggerFactory: FACTORY_LOG
  protected def cliFactory: FACTORY_CLI = CLA.Factory()
  protected def makeCli(args:        List[String]): C
  final protected def buildEnv(args: C): ZLayer[zio.ZEnv, Throwable, BaseEnv[C]] = {
    loggerFactory.assembleLogger >+>
      cliFactory.assembleCliBuilder(args)
  }

  // Build user environment
  protected def env: ZLayer[ZIO_ENV, Throwable, ENV]

  // Core business logic
  protected def runApp(): ZIO[COMPLETE_ENV, Throwable, OUTPUT]

  // Default implementations
  protected def displayCommandLines: Boolean = true


  protected def stopSparkAtTheEnd: Boolean = true

  // RUNTIME
  protected def makePlatform: Platform = {
    Platform.default
      .withReportFailure { cause =>
        if (cause.died) println(cause.prettyPrint)
      }
  }
  protected def makeRuntime: BootstrapRuntime = new BootstrapRuntime {
    override val platform: Platform = makePlatform
  }

  def processErrors(f: Throwable): Option[String] = {
    
    f.printStackTrace(System.out)

    f match {
      case WriteError(error)       => Some(error)
      case _: InterruptedException => Some("0")
      case _ => Some("1")
    }
  }

  private object ErrorProcessing {
    def unapply(e: Throwable): Option[String] = {
      processErrors(e)
    }
  }

  protected def app: ZIO[COMPLETE_ENV, Throwable, OUTPUT] = {
    for {
      _ <- if (displayCommandLines) CLA.displayCommandLines[C]() else UIO(())
      output <- runApp()

    } yield {
      output
    }
  }

  protected def run(args: List[String]): ZIO[zio.ZEnv, Nothing, String] = {
    Task(makeCli(args))
      .map(buildEnv)
      .flatMap { baseEnv =>
        app
          .provideSomeLayer[zio.ZEnv with BaseEnv[C]](env)
          .provideCustomLayer(baseEnv)
      }
      .catchSome { case h: HelpHandlerException => h.printHelpMessage }
      .fold(
        {
          case CLA.Helper.ErrorParser(code) => code.toString
          case ErrorProcessing(error)   =>  error
          case _         => "1"
        },
        {
          case _        => "0"
        }
      )
  }

  final def main(args: Array[String]): Unit = {
    val runtime = makeRuntime
    val exitCode = runtime.unsafeRun(run(args.toList))
    println(s"ExitCode: $exitCode")
  }


object ZioApp:
  type BaseEnv[C <: CLA.Service] = CLA.CommandLineArguments[C] with Logger
  type ZIOEnv[C <: CLA.Service] = zio.ZEnv with BaseEnv[C]
