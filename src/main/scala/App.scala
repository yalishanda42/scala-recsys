import cats.effect.{ExitCode, IO, IOApp}

object RecommenderApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    args.match {
      case List("train") =>
        IO(println("Training mode."))
          .as(ExitCode.Success)
      case List("predict") =>
        IO(println("Predicting mode."))
          .as(ExitCode.Success)
      case _ =>
        IO(System.err.println("Usage: RecommenderApp train|predict"))
          .as(ExitCode.Error)
    }
}