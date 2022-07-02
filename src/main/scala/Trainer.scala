import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}

class Trainer {
  def prepareData(sc: SparkContext)(dataPath: String): RDD[Rating] =
    sc.textFile(dataPath).map { line => line.split('\t') match {
      case Array(user, item, rate, _) => Rating(user.toInt, item.toInt, rate.toDouble)
    }}
}