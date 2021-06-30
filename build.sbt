val scala2Version = "2.13.4"
val scala3Version = "3.0.0-M3"

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "Coding Challenge",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
                                "com.univocity" % "univocity-parsers" % "2.9.1",
                                "dev.zio" %% "zio" % "1.0.4-2",
                                "dev.zio" %% "zio-streams" % "1.0.4-2",
                                "com.softwaremill.sttp.client3" % "async-http-client-backend_3.0.0-M3" % "3.1.1",
                                "com.softwaremill.sttp.client3" % "zio_3.0.0-M3" % "3.1.1",
                                "com.softwaremill.sttp.client3" % "core_3.0.0-M3" % "3.1.1",
                                "org.rogach" % "scallop_3.0.0-M3" % "4.0.2",
                                "org.scalatest" %% "scalatest" % "3.2.3" % Test


    )
  )
  



lazy val commonSettings = Seq(
  version := "0.1.0",
 
)

