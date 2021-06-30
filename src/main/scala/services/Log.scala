package services

import zio.Task
import zio.console.Console
import services.internal.Logger

case class Log(console: Console.Service) extends Logger.Service {
  override def info(txt: => String): Task[Unit] =
    console.putStrLn(s"INFO: $txt")

  override def error(txt: => String): Task[Unit] =
    console.putStrLn(s"ERROR: $txt")

  override def debug(txt: => String): Task[Unit] =
    console.putStrLn(s"DEBUG: $txt")
}