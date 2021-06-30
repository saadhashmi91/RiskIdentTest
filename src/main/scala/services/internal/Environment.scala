package services.internal

import org.rogach.scallop.{ArgType, DefaultConverters, ValueConverter}

enum Environment:
  case Local
  def parseEnv(input: String): Option[Environment] = {
    input.trim.toLowerCase match
     case "local" => Some(Local)
  }
end Environment

object Environment:
 given EnvironmentParser: ValueConverter[Environment] = EnvironmentConverter.Parser



object EnvironmentConverter extends DefaultConverters:
  type ArgType = Environment

  val Parser: ValueConverter[EnvironmentConverter.ArgType] =
    new ValueConverter[EnvironmentConverter.ArgType] {
      def parse(
        s: List[(String, List[String])]
      ): Either[String, Option[EnvironmentConverter.ArgType]] = {
        s match {
          case (_, i :: Nil) :: Nil =>
            Environment.Local.parseEnv(i) match {
              case Some(e) => Right(Some(e))
              case None    => Left(s"Cannot find valid environment for input: '$i'")
            }
          case Nil => Right(None)
          case _   => Left("you should provide exactly one argument for this option")
        }
      }
      // Scallop Argument Type : SINGLE Option takes only one argument. (for example, .opt[Int])
      val argType: ArgType.V = ArgType.SINGLE
    }