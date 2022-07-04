package traits

import org.apache.spark.rdd.RDD
import scala.util.Try

case class Split[RowType](train: RDD[RowType], test: RDD[RowType])

trait DataTransformer[RowType]:
  def preprocess(data: RDD[String]): RDD[RowType]
  def split(data: RDD[RowType]): Split[RowType]
