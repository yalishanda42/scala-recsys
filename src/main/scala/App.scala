import cats.effect.{ExitCode, IO, IOApp}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import scala.util.{Try, Success, Failure}
import scala.reflect.io.Directory
import java.io.File

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
  val dataPath = "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/u.data"
  val basePath = "/Users/yalishanda/Documents/scala-recsys/data/ml-100k/ALSmodel"
  val modelPath = s"$basePath/model"
  val checkpointPath = s"$basePath/checkpoint"

  def run(args: List[String]): IO[ExitCode] =
    args.match
      case List("train") =>
        SparkProvider.sparkContext("Training").use { sc =>
          for {
            data <- loadData(sc)
            split <- IO(algo.transformer.split(data))
            model <- train(sc, split.train)
            - <- saveModel(sc, model, modelPath)
            loadedModel <- loadModel(sc, modelPath)
            rmse <- test(sc, loadedModel, split.test)
            _ <- logger.logInfo(s"RMSE: $rmse")
          } yield ExitCode.Success
        }

      case List("test") =>
        SparkProvider.sparkContext("Testing").use { sc =>
          for {
            data <- loadData(sc)
            model <- loadModel(sc, modelPath)
            rmse <- test(sc, model, data)
            _ <- logger.logInfo(s"RMSE: $rmse")
          } yield ExitCode.Success
        }

      case List("predict") =>
        logger.logInfo("Predicting mode.")
          .as(ExitCode.Success)

      case _ =>
        logger.logError("Usage: RecommenderApp train|test|predict")
          .as(ExitCode.Error)


  def loadData(sc: SparkContext): IO[RDD[Rating]] =
    for {
      _ <- logger.logInfo("Loading data...")
      _ <- IO(sc.setCheckpointDir(checkpointPath))
      rawData <- IO(sc.textFile(dataPath))
      data <- IO(algo.transformer.preprocess(rawData))
      _ <- IO(data.checkpoint)
    } yield data

  def train(sc: SparkContext, data: RDD[Rating]): IO[MatrixFactorizationModel] =
    for {
      _ <- logger.logInfo("Training model...")
      model <- IO(algo.trainer.train(data))
    } yield model

  def saveModel(sc: SparkContext, model: MatrixFactorizationModel, path: String): IO[Unit] =
    for {
      _ <- logger.logInfo(s"Deleting $path...")
      _ <- IO(new Directory(new File(path)).deleteRecursively())
      _ <- logger.logInfo(s"Saving model to $path...")
      _ <- IO(model.save(sc, modelPath))
    } yield ()

  def loadModel(sc: SparkContext, path: String): IO[MatrixFactorizationModel] =
    for {
      _ <- logger.logInfo(s"Loading model from $path...")
      model <- IO(MatrixFactorizationModel.load(sc, path))
    } yield model

  def test(
    sc: SparkContext,
    model: MatrixFactorizationModel,
    data: RDD[Rating],
    metric: Metric = RMSE()
  ): IO[Double] =
    for {
      _ <- logger.logInfo("Testing model...")
      rmse <- IO(algo.tester.test(model, metric, data))
    } yield rmse
