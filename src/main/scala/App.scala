import cats.effect.{ExitCode, IO, IOApp}
import cats.data.EitherT
import cats.implicits.*
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import scala.util.{Try, Success, Failure}
import scala.reflect.io.Directory
import java.io.File

import utils.*
import traits.*
import shared.testables.*
import metrics.*
import domains.movielens.algorithms.*
import domains.movielens.datatransformers.*
import domains.restaurants.algorithms.*
import domains.restaurants.datatransformers.*
import domains.books.algorithms.*
import domains.books.datatransformers.*
import registry.AlgorithmsRegistry

enum RecommendationMode:
  case RecommendUsers
  case RecommendItems

object RecommendationMode:
  def unapply(rawValue: String): Option[RecommendationMode] = rawValue.trim.toLowerCase match
    case "-u" => Some(RecommendUsers)
    case "-i" => Some(RecommendItems)
    case _ => None

enum Subcommand:
  case Train
  case Test
  case Recommend(mode: RecommendationMode, id: String)

object Subcommand:
  def apply(rawValue: List[String]): Option[Subcommand] = rawValue.map(_.trim.toLowerCase) match
    case List("train") => Some(Train)
    case List("test") => Some(Test)
    case List("recommend", RecommendationMode(mode), id) => Some(Recommend(mode, id))
    case _ => None

case class ArgumentParser(args: List[String]):
  lazy val algorithm = args.lift(0).flatMap(AlgorithmsRegistry.apply)
  lazy val subcommand = Subcommand(args.drop(1).dropRight(2))
  lazy val dataPath = args.lift(args.length - 2) // second to last
  lazy val modelBasePath = args.last.some

object RecommenderApp extends IOApp:

  val logger = Logger("===> [RecommenderApp]")

  def modelSubpath(basePath: String) = s"$basePath/model"
  def checkpointSubpath(basePath: String) = s"$basePath/checkpoint"

  def run(args: List[String]): IO[ExitCode] =
    val parser = ArgumentParser(args)
    val result = for {
      s <- parser.subcommand
      a <- parser.algorithm
      d <- parser.dataPath
      m <- parser.modelBasePath
    } yield {
      SparkProvider.sparkContext("RecommenderApp").use { sc =>
        runSubcommand(sc, s, a, d, m).attemptT.value
      }
    }

    result match
      case Some(ioOfEither) =>
        ioOfEither.flatMap {
          case Right(_) =>
            logger.logInfo("Done.").as(ExitCode.Success)
          case Left(error) =>
            logger.logError(error.getMessage).as(ExitCode.Error)
        }
      case None =>
        logger.logError("Usage: RecommenderApp domain-v? train|test|predict [-u|-i <id>] dataPath modelBasePath")
          .as(ExitCode.Error)

  def runSubcommand(
    sc: SparkContext,
    subcommand: Subcommand,
    algo: Algorithm[Rating, MatrixFactorizationModel],
    dataPath: String,
    modelBasePath: String
  ): IO[Unit] =
    val modelPath = modelSubpath(modelBasePath)
    subcommand match
      case Subcommand.Train =>
        for {
          data <- loadData(sc, dataPath, modelBasePath, algo.transformer)
          split <- IO(algo.transformer.split(data))
          model <- train(sc, split.train, algo.trainer)
          _ <- saveModel(sc, model, modelPath)
          loadedModel <- loadModel(sc, modelPath)
          rmse <- test(sc, loadedModel, split.test, algo.tester)
          result <- logger.logInfo(s"RMSE: $rmse")
        } yield result

      case Subcommand.Test =>
        for {
          data <- loadData(sc, dataPath, modelBasePath, algo.transformer)
          model <- loadModel(sc, modelPath)
          rmse <- test(sc, model, data, algo.tester)
          result <- logger.logInfo(s"RMSE: $rmse")
        } yield result

      case Subcommand.Recommend(mode, id) =>
        for {
          model <- loadModel(sc, modelPath)
          recommendations <- recommend(sc, model, mode, id)
          result <- logger.logInfo(s"Recommendations: $recommendations")
        } yield result

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
    mode: RecommendationMode,
    id: String
  ): IO[List[Rating]] =
    mode match
      case RecommendationMode.RecommendItems =>
        recommendItems(sc, model, id)
      case RecommendationMode.RecommendUsers =>
        recommendUsers(sc, model, id)

  def recommendItems(sc: SparkContext, model: MatrixFactorizationModel, id: String): IO[List[Rating]] =
    val count = 10
    for {
      _ <- logger.logInfo(s"Recommending $count items for user $id...")
      recommendations <- IO(model.recommendProducts(id.toInt, count))
    } yield recommendations.toList

  def recommendUsers(sc: SparkContext, model: MatrixFactorizationModel, id: String): IO[List[Rating]] =
    val count = 10
    for {
      _ <- logger.logInfo(s"Recommending $count users for item $id...")
      recommendations <- IO(model.recommendUsers(id.toInt, count))
    } yield recommendations.toList
