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
import domains.movielens.datatransformers.*
import domains.restaurants.algorithms.*
import domains.restaurants.datatransformers.*
import domains.books.algorithms.*
import domains.books.datatransformers.*
import registry.AlgorithmsRegistry

object RecommenderApp extends IOApp:

  val logger = Logger("===> [RecommenderApp]")

  // val dataPath = "/Users/yalishanda/Documents/scala-recsys/data/books/ratings.csv"
  // val basePath = "/Users/yalishanda/Documents/scala-recsys/data/books/model"
  def modelSubpath(basePath: String) = s"$basePath/model"
  def checkpointSubpath(basePath: String) = s"$basePath/checkpoint"

  def run(args: List[String]): IO[ExitCode] =
    args.match
      case List(algorithm, "train", dataPath, modelBasePath) =>
        val algo = AlgorithmsRegistry(algorithm)
        val modelPath = modelSubpath(modelBasePath)
        SparkProvider.sparkContext("Training").use { sc =>
          for {
            data <- loadData(sc, dataPath, modelBasePath, algo.transformer)
            split <- IO(algo.transformer.split(data))
            model <- train(sc, split.train, algo.trainer)
            _ <- saveModel(sc, model, modelPath)
            loadedModel <- loadModel(sc, modelPath)
            rmse <- test(sc, loadedModel, split.test, algo.tester)
            _ <- logger.logInfo(s"RMSE: $rmse")
          } yield ExitCode.Success
        }

      case List(algorithm, "test", dataPath, modelBasePath) =>
        val algo = AlgorithmsRegistry(algorithm)
        val modelPath = modelSubpath(modelBasePath)
        SparkProvider.sparkContext("Testing").use { sc =>
          for {
            data <- loadData(sc, dataPath, modelBasePath, algo.transformer)
            model <- loadModel(sc, modelPath)
            rmse <- test(sc, model, data, algo.tester)
            _ <- logger.logInfo(s"RMSE: $rmse")
          } yield ExitCode.Success
        }

      case List(algorithm, "recommend", mode, id, dataPath, modelBasePath) =>
        val algo = AlgorithmsRegistry(algorithm)
        val modelPath = modelSubpath(modelBasePath)
        SparkProvider.sparkContext("Recommending").use { sc =>
          for {
            model <- loadModel(sc, modelPath)
            recommendations <- recommend(sc, model, mode, id)
            _ <- logger.logInfo(s"Recommendations: $recommendations")
          } yield ExitCode.Success
        }

      case _ =>
        logger.logError("Usage: RecommenderApp domain-v? train|test|predict [-u|-m <id>] dataPath modelBasePath")
          .as(ExitCode.Error)


  def loadData(
    sc: SparkContext,
    dataPath: String,
    modelBasePath: String,
    transformer: DataTransformer[Rating]
  ): IO[RDD[Rating]] =
    for {
      _ <- logger.logInfo("Loading data...")
      _ <- IO(sc.setCheckpointDir(checkpointSubpath(modelBasePath)))
      rawData <- IO(sc.textFile(dataPath))
      data <- IO(transformer.preprocess(rawData))
      _ <- IO(data.checkpoint)
    } yield data

  def train(
    sc: SparkContext,
    data: RDD[Rating],
    trainer: Trainable[Rating, MatrixFactorizationModel]
  ): IO[MatrixFactorizationModel] =
    for {
      _ <- logger.logInfo("Training model...")
      model <- IO(trainer.train(data))
    } yield model

  def saveModel(sc: SparkContext, model: MatrixFactorizationModel, modelPath: String): IO[Unit] =
    for {
      _ <- logger.logInfo(s"Deleting $modelPath...")
      _ <- IO(new Directory(new File(modelPath)).deleteRecursively())
      _ <- logger.logInfo(s"Saving model to $modelPath...")
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
    tester: Testable[Rating, MatrixFactorizationModel],
    metric: Metric = RMSE()
  ): IO[Double] =
    for {
      _ <- logger.logInfo("Testing model...")
      rmse <- IO(tester.test(model, metric, data))
    } yield rmse

  def recommend(
    sc: SparkContext,
    model: MatrixFactorizationModel,
    mode: String,
    id: String
  ): IO[List[Rating]] =
    mode match
      case "-u" =>
        recommendMovies(sc, model, id)
      case "-m" =>
        recommendUsers(sc, model, id)
      case other =>
        logger.logError(s"Unrecognized option $other!\nUsage: RecommenderApp recommend -u|-m <id>")
          .as(List())

  def recommendMovies(sc: SparkContext, model: MatrixFactorizationModel, id: String): IO[List[Rating]] =
    val count = 10
    for {
      _ <- logger.logInfo(s"Recommending $count movies for user $id...")
      recommendations <- IO(model.recommendProducts(id.toInt, count))
    } yield recommendations.toList

  def recommendUsers(sc: SparkContext, model: MatrixFactorizationModel, id: String): IO[List[Rating]] =
    val count = 10
    for {
      _ <- logger.logInfo(s"Recommending $count users for movie $id...")
      recommendations <- IO(model.recommendUsers(id.toInt, count))
    } yield recommendations.toList
