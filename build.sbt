name := "maschinendeck-spaceapi"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % "1.0.0-M23",
  "org.http4s" %% "http4s-blaze-server" % "1.0.0-M23",
  "org.http4s" %% "http4s-circe" % "1.0.0-M23",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
)

scalacOptions += "-deprecation"

assembly / assemblyJarName := "maschinendeck-spaceapi.jar"

assembly / mainClass := Some("Main")

assembly / test := {}
