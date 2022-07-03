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
          val result = for {
            _ <- IO(println("Preparing and checkpointing data..."))
            _ <- IO(sc.setCheckpointDir("/Users/yalishanda/Documents/scala-recsys/data/ml-100k/checkpoint"))
            data <- IO(trainer.prepareData(Utils.stringToRatingMapper))
            _ <- IO(data.checkpoint)
            _ <- IO(println("Training model..."))
            model <- IO(trainer.train(data))
            _ <- IO(println("Saving model..."))
            result <- IO(trainer.saveModel(model))
          } yield (result)
          result.flatMap { r => r match
            case Success(_) =>
              IO(println("Model saved successfully!")).map(_ => ExitCode.Success)
            case Failure(e) =>
              IO(System.err.println(s"Error: ${e.getMessage}")).map(_ => ExitCode.Error)
          }
        }

      case List("predict") =>
        IO(println("Predicting mode."))
          .as(ExitCode.Success)

      case _ =>
        IO(System.err.println("Usage: RecommenderApp train|predict"))
          .as(ExitCode.Error)
