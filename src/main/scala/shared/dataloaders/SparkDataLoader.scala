package shared.dataloaders

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import scala.util.Try

import traits.DataLoader

class SparkDataLoader(sc: SparkContext) extends DataLoader[String] {
  def loadData(path: String): RDD[String] =
    sc.textFile(path)
}
