import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

object Metrics:
  def rmse(actual: RDD[Rating], predicted: RDD[Rating]): Double = {
    val indexedActual = actual.zipWithIndex.map { case (v, i) => i -> v }
    val indexedPredicted = predicted.zipWithIndex.map { case (v, i) => i -> v }

    val diff = indexedActual.join(indexedPredicted).map {
      case (_, (a, p)) => math.pow(a.rating - p.rating, 2)
    }
    math.sqrt(diff.sum / diff.count)
  }