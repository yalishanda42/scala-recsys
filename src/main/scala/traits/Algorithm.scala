package traits

import org.apache.spark.mllib.util.Saveable

trait Algorithm[RowType, ModelType <: Saveable]:
  def transformer: DataTransformer[RowType]
  def trainer: Trainable[RowType, ModelType]
  def tester: Testable[RowType, ModelType]
