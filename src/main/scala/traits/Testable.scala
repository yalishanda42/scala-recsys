package traits

import org.apache.spark.rdd.RDD

import metrics.Metric

trait Testable[RowType, ModelType]:
  def test(model: ModelType, metric: Metric, actualData: RDD[RowType]): Double
