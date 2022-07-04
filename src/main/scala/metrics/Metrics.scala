package metrics

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

sealed trait Metric:
  def evaluate: Double

case class RMSE(actual: RDD[Rating], predicted: RDD[Rating]) extends Metric:
  def evaluate: Double =
    val indexedActual = actual.zipWithIndex.map { case (v, i) => i -> v }
    val indexedPredicted = predicted.zipWithIndex.map { case (v, i) => i -> v }

    val diff = indexedActual.join(indexedPredicted).map {
      case (_, (a, p)) => math.pow(a.rating - p.rating, 2)
    }
    math.sqrt(diff.sum / diff.count)
