package shared.testables

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

import traits.Testable
import metrics.Metric

case class MatrixFactorizationModelTester() extends Testable[Rating, MatrixFactorizationModel]:
  def test(model: MatrixFactorizationModel, metric: Metric, actualData: RDD[Rating]): Double =
    val predictedData = model.predict(actualData.map(r => (r.user, r.product)))
    metric.evaluate(actualData, predictedData)
