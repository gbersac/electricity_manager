name := """electricity_manager"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"
lazy val doobieVersion = "0.3.0"

libraryDependencies ++= Seq(
  "com.github.mauricio" %% "postgresql-async" % "0.2.20",
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)
