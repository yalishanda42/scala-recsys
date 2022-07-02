name := "scala-recsys"
version := "0.1"

scalaVersion := "3.1.1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "org.typelevel" %% "cats-core" % "2.7.0",
  "org.typelevel" %% "cats-effect" % "3.3.12",
  "co.fs2" %% "fs2-core" % "3.2.7",
  "co.fs2" %% "fs2-io" % "3.2.7",
  "co.fs2" %% "fs2-reactive-streams" % "3.2.7",
  "org.apache.spark" %% "spark-core" % "3.2.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.apache.spark" %% "spark-mllib" % "3.2.0",
  "org.apache.spark" %% "spark-sql" % "3.2.0",
  "org.apache.spark" %% "spark-hive" % "3.2.0",
)
