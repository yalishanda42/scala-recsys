import cats.effect.{ExitCode, IO, IOApp}
import scala.util.{Try, Success, Failure}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel

import utils.*
import traits.*
import shared.dataloaders.*
import metrics.*
import domains.movielens.algorithms.*
import domains.movielens.dataloaders.*

object RecommenderApp extends IOApp:

  // TODO: use argv
  val dataPath =  "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/u.data"
  val modelPath = "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/ALSmodel"

  def run(args: List[String]): IO[ExitCode] =
    args.match
      case List("train") =>
        SparkProvider.sparkContext("Training").use(train)

      case List("test") =>
        SparkProvider.sparkContext("Testing").use(test)

      case List("predict") =>
        IO(println("Predicting mode."))
          .as(ExitCode.Success)

      case _ =>
        IO(System.err.println("Usage: RecommenderApp train|predict"))
          .as(ExitCode.Error)


  def train(sc: SparkContext): IO[ExitCode] =
    val loader = new MovieLensRatingsLoader(sc)
    val trainer = new MovieRecommenderV1
    val result = for {
      _ <- IO(println("Preparing data..."))
      _ <- IO(sc.setCheckpointDir("/Users/yalishanda/Documents/scala-recsys/data/ml-100k/checkpoint"))
      data <- IO(loader.loadData(dataPath))
      _ <- IO(data.checkpoint)
      _ <- IO(println("Training model..."))
      model <- IO(trainer.train(data))
      _ <- IO(println("Saving model..."))
      result <- IO(Try(model.save(sc, modelPath)))
    } yield result

    result.flatMap {
      case Success(_) =>
        IO(println("Model saved successfully!")).as(ExitCode.Success)
      case Failure(e) =>
        IO(System.err.println(s"Error: ${e.getMessage}")).as(ExitCode.Error)
    }

  def test(sc: SparkContext): IO[ExitCode] =
    val loader = new MovieLensRatingsLoader(sc)
    val result = for {
      model <- Try(MatrixFactorizationModel.load(sc, modelPath))
      dataset <- Try(loader.loadData(dataPath))
      _ <- Try(println("Testing model..."))
      predictions <- Try(model.predict(dataset.map(r => (r.user, r.product))))
      rmse <- Try(RMSE(dataset, predictions).evaluate)
    } yield rmse

    result match
      case Success(r) =>
        IO(println(s"RMSE: $r")).as(ExitCode.Success)
      case Failure(e) =>
        IO(System.err.println(s"Error: ${e.getMessage}")).as(ExitCode.Error)

