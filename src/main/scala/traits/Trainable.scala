package traits

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.Saveable

trait Trainable[RowType, ModelType <: Saveable]:
  def train(data: RDD[RowType]): ModelType
