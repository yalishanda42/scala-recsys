lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "bg.yalishanda",
      scalaVersion := "3.1.1"
    )),
    name := "scala-recsys"
  )

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % Test
