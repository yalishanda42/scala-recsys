import cats.effect.{ExitCode, IO, IOApp}
import scala.util.{Success, Failure}

object RecommenderApp extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    args.match
      case List("train") =>
        SparkProvider.sparkContext("Trainer").use { sc =>
          val trainer = Trainer(
            sc,
            "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/u.data",
            "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/ALSmodel"
          )
          for {
            _ <- IO(println("Training model..."))
            model <- IO(trainer.train(Utils.stringToRatingMapper))
            _ <- IO(println("Saving model..."))
            result <- IO(trainer.saveModel(model))
          } yield (
            result match
              case Success(_) =>
                ExitCode.Success
              case Failure(e) =>
                ExitCode.Error
          )
        }
      case List("predict") =>
        IO(println("Predicting mode."))
          .as(ExitCode.Success)
      case _ =>
        IO(System.err.println("Usage: RecommenderApp train|predict"))
          .as(ExitCode.Error)
