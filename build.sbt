name := "try-es"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq("com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.4",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.9",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
)

//"com.typesafe.play" %% "play-ws-standalone" % "1.1.6"
