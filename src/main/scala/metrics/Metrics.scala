package metrics

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

sealed trait Metric:
  def evaluate(actualData: RDD[Rating], predictedData: RDD[Rating]): Double

case class RMSE() extends Metric:
  def evaluate(actualData: RDD[Rating], predictedData: RDD[Rating]): Double =
    val indexedActual = actualData.zipWithIndex.map { case (v, i) => i -> v }
    val indexedPredicted = predictedData.zipWithIndex.map { case (v, i) => i -> v }

    val diff = indexedActual.join(indexedPredicted).map {
      case (_, (a, p)) => math.pow(a.rating - p.rating, 2)
    }
    math.sqrt(diff.sum / diff.count)
