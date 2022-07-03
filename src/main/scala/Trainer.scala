import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import scala.util.Try

class Trainer(sc: SparkContext, dataPath: String, modelPath: String):

  def prepareData(lineMapper: String => Rating): RDD[Rating] =
    sc.textFile(dataPath).map(lineMapper)

  def train(
    ratings: RDD[Rating],
    rank: Int = 50,
    iteration: Int = 20,
    lambda: Double = 0.1
  ): MatrixFactorizationModel =
    ALS.train(ratings, rank, iteration, lambda) // TODO: allow for different algos

  def saveModel(model: MatrixFactorizationModel): Try[Unit] =
    Try(model.save(sc, modelPath))
