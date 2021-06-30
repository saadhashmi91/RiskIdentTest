package services

import services.internal.{CommandLineArguments,_}
import org.rogach.scallop.{intConverter}

import org.rogach.scallop.{ScallopConf, ScallopOption}
import zio.ZIO

case class Arguments(input: List[String])
  extends ScallopConf(input) with CommandLineArguments.Service {
  val lines: ScallopOption[Int] = opt[Int](
    required = true,
    noshort = false
  )
}

object Arguments {
  def apply[A](f: Arguments => A): ZIO[CommandLineArguments.CommandLineArguments[Arguments], Throwable, A] = {
    CommandLineArguments.get[Arguments].apply(f)
  }
}