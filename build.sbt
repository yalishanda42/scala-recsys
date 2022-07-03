name := "scala-recsys"
version := "0.1"

scalaVersion := "3.1.1"

libraryDependencies ++= Seq(
  ("org.scalatest" %% "scalatest" % "3.2.9" % Test).cross(CrossVersion.for3Use2_13),
  ("org.typelevel" %% "cats-core" % "2.7.0").cross(CrossVersion.for3Use2_13),
  ("org.typelevel" %% "cats-effect" % "3.3.12").cross(CrossVersion.for3Use2_13),
  ("co.fs2" %% "fs2-core" % "3.2.7").cross(CrossVersion.for3Use2_13),
  ("co.fs2" %% "fs2-io" % "3.2.7").cross(CrossVersion.for3Use2_13),
  ("co.fs2" %% "fs2-reactive-streams" % "3.2.7").cross(CrossVersion.for3Use2_13),
  ("org.apache.spark" %% "spark-core" % "3.3.0" % "provided").cross(CrossVersion.for3Use2_13),
  ("org.apache.spark" %% "spark-mllib" % "3.3.0" % "provided").cross(CrossVersion.for3Use2_13),
  ("org.apache.spark" %% "spark-sql" % "3.3.0" % "provided").cross(CrossVersion.for3Use2_13),
  ("org.apache.spark" %% "spark-hive" % "3.3.0" % "provided").cross(CrossVersion.for3Use2_13),
)

// include the 'provided' Spark dependency on the classpath for `sbt run`
Compile / run := Defaults.runTask(Compile / fullClasspath, Compile / run / mainClass, Compile / run / runner).evaluated
