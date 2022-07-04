import cats.effect.{ExitCode, IO, IOApp}
import scala.util.{Try, Success, Failure}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import utils.*
import traits.*
import shared.dataloaders.*
import shared.testables.*
import metrics.*
import domains.movielens.algorithms.*
import domains.movielens.dataloaders.*

object RecommenderApp extends IOApp:

  val logger = Logger("===> [RecommenderApp]")

  // TODO: use argv
  val algo: Algorithm[Rating, MatrixFactorizationModel] = MovieRecommenderV1()
  val dataPath =  "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/u.data"
  val modelPath = "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/ALSmodel"

  def run(args: List[String]): IO[ExitCode] =
    args.match
      case List("train") =>
        SparkProvider.sparkContext("Training").use(train)

      case List("test") =>
        SparkProvider.sparkContext("Testing").use(test)

      case List("predict") =>
        logger.logInfo("Predicting mode.")
          .as(ExitCode.Success)

      case _ =>
        logger.logError("Usage: RecommenderApp train|test|predict")
          .as(ExitCode.Error)


  def train(sc: SparkContext): IO[ExitCode] =
    val result = for {
      _ <- logger.logInfo("Preparing data...")
      _ <- IO(sc.setCheckpointDir(s"$modelPath/checkpoint"))
      rawData <- IO(sc.textFile(dataPath))
      data <- IO(algo.transformer.preprocess(rawData))
      _ <- IO(data.checkpoint)
      split <- IO(algo.transformer.split(data))
      _ <- logger.logInfo("Training model...")
      model <- IO(algo.trainer.train(split.train))
      _ <- logger.logInfo("Saving model...")
      result <- IO(Try(model.save(sc, modelPath)))
    } yield result

    result.flatMap {
      case Success(_) =>
        logger.logInfo("Model saved successfully!").as(ExitCode.Success)
      case Failure(e) =>
        logger.logError(s"Error: ${e.getMessage}").as(ExitCode.Error)
    }

  def test(sc: SparkContext): IO[ExitCode] =
    val result = for {
      model <- Try(MatrixFactorizationModel.load(sc, modelPath))
      rawDataset <- Try(sc.textFile(dataPath))
      processedDataSet <- Try(algo.transformer.preprocess(rawDataset))
      _ <- Try(logger.logInfo("Testing model..."))
      rmse <- Try(algo.tester.test(model, RMSE(), processedDataSet))
    } yield rmse

    result match
      case Success(r) =>
        logger.logInfo(s"RMSE: $r").as(ExitCode.Success)
      case Failure(e) =>
        logger.logError(s"Error: ${e.getMessage}").as(ExitCode.Error)

