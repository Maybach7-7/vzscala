name := "rock-paper-scissors-server"
version := "1.0.0"
scalaVersion := "3.2.2"

val AkkaVersion = "2.8.4"
val AkkaHttpVersion = "10.5.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "io.spray" %% "spray-json" % "1.3.6",
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)

// Assembly settings for creating a fat JAR
assembly / mainClass := Some("rockpaperscissors.MainServer")
assembly / assemblyJarName := "rock-paper-scissors-server.jar"
assembly / test := {}