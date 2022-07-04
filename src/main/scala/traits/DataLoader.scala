package traits

import org.apache.spark.rdd.RDD
import scala.util.Try

trait DataLoader[RowType] {
  def loadData(path: String): RDD[RowType]
}
