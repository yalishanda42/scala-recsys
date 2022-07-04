package domains.movielens.dataloaders

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating
import scala.util.Try

import traits.DataLoader
import shared.dataloaders.SparkDataLoader

case class MovieLensRatingsLoader(sc: SparkContext) extends DataLoader[Rating]:
  private val sparkLoader = new SparkDataLoader(sc)

  def loadData(path: String): RDD[Rating] =
    sparkLoader.loadData(path).map { line =>
      val fields = line.split('\t')
      Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble)
    }
