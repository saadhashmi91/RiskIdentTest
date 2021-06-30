package services.internal

//import com.leobenkel.zparkio.Env._
import scala.collection.immutable.ArraySeq
import services.internal.Logger.Logger
import izumi.reflect.Tag
import org.rogach.scallop.exceptions.{Help, ScallopException}
import org.rogach.scallop.{Scallop, ScallopConf, ScallopOption}
import zio.console.Console
import zio.{Has, Task, UIO, ZIO, ZLayer, console}

import scala.util.Try

object CommandLineArguments {
  import Helper._

  type CommandLineArguments[C <: CommandLineArguments.Service] = Has[C]

  trait Service extends ScallopConf {
    this.appendDefaultToDescription = true

    final override def verify(): Unit =
      throw new Exception(s"Do not call verify yourself! Zparkio calls it for you.")

    private[internal] def verifyInternal(): Task[this.type] = {
      ZIO.fromTry(Try {
        super.verify()
        this
      })
    }

    lazy final private[internal] val commandsDebug: Seq[String] = {
      val (active, inactive) = filteredSummary(Set.empty)
        .split('\n')
        .partition(_.trim.startsWith("*"))

       ArraySeq.unsafeWrapArray((active.sorted :+ "") ++ inactive.sorted)
    }

    final val env: ScallopOption[Environment] = opt[Environment](
      required = true,
      noshort = true,
      default = Some(Environment.Local),
      descr = "Set the environment for the run."
    )

    lazy final protected val getAllMetrics: Seq[(String, Any)] = {
      builder.opts.map(o => (o.name, builder.get(o.name).getOrElse("<NONE>")))
    }

    final override def onError(e: Throwable): Unit = e match {
      case Help("") =>
        throw HelpHandlerException(builder, None)
      case Help(subCommand) =>
        throw HelpHandlerException(builder.findSubbuilder(subCommand).get, Some(subCommand))
      case other => throw other
    }
  }

  object Helper {
    private[internal] case class HelpHandlerException(
      s:          Scallop,
      subCommand: Option[String]
    ) extends Throwable {
      private def print(msg: String): ZIO[Console, Throwable, Unit] = console.putStr(msg)

      lazy private val header: String = subCommand match {
        case None    => "Help:"
        case Some(s) => s"Help for '$s':"
      }

      def printHelpMessage: ZIO[zio.ZEnv, Throwable, Unit] = {
        for {
          _ <- print(header)
          _ <- ZIO.foreach(s.vers)(print)
          _ <- ZIO.foreach(s.bann)(print)
          _ <- print(s.help)
          _ <- ZIO.foreach(s.foot)(print)
        } yield {
          ()
        }
      }
    }

    private[internal] object ErrorParser {
      def unapply(e: Throwable): Option[Int] = e match {
        case _: Help                 => Some(0)
        case _: HelpHandlerException => Some(0)
        case _ => None
      }
    }

    type ZIO_CONFIG_SERVICE[A <: CommandLineArguments.Service] =
      ZIO[CommandLineArguments[A], Throwable, YourConfigWrapper[A]]

    implicit class Shortcut[C <: CommandLineArguments.Service](z: ZIO_CONFIG_SERVICE[C]) {
      def apply[A](f: C => A): ZIO[CommandLineArguments[C], Throwable, A] = z.map(_.apply(f))
    }

    case class YourConfigWrapper[C <: CommandLineArguments.Service](config: C) {
      def apply[A](f: C => A): A = f(config)
    }

  }

  private[internal] trait Factory[C <: CommandLineArguments.Service] {
    private[internal] def createCliSafely(args: C): ZIO[Any, Throwable, C] = {
      args.verifyInternal()
    }

    private[internal] def assembleCliBuilder(
      args: C
    )(
      implicit t: Tag[Has[C]]
    ): ZLayer[Logger, Throwable, CommandLineArguments[C]] =
      ZLayer.fromServiceM { logger =>
        createCliSafely(args).tapError {
          case cliError: ScallopException => Logger.displayAllErrors(cliError).provide(Has(logger))
          case _ => UIO(())
        }
      }
  }

  private[internal] object Factory {
    private[internal] def apply[C <: CommandLineArguments.Service](): Factory[C] = new Factory[C] {}
  }

  def apply[C <: CommandLineArguments.Service](
  )(//  Tag[Has[Svc[A]]
    implicit t: Tag[Has[C]]
  ): ZIO[CommandLineArguments[C], Throwable, C] = {
    ZIO.service[C]
  }

  def get[C <: CommandLineArguments.Service]: ZIO_CONFIG_SERVICE[C] = {
    apply[C]()
      .flatMap(_.verifyInternal())
      .map(YourConfigWrapper[C])
  }

  def displayCommandLines[C <: CommandLineArguments.Service](
  ): ZIO[CommandLineArguments[C] with Logger, Throwable, Unit] = {
    for {
      conf <- apply[C]()
      _    <- Logger.info("--------------Command Lines--------------")
      _    <- ZIO.foreach(conf.commandsDebug)(s => Logger.info(s))
      _    <- Logger.info("-----------------------------------------")
      _    <- Logger.info("")
    } yield {}
  }
}
