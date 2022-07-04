package domains.movielens.datatransformers

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating
import scala.util.Try

import traits.{DataTransformer, Split}

case class MovieLensTransformerV1() extends DataTransformer[Rating]:

  def preprocess(data: RDD[String]): RDD[Rating] =
    data.map { line =>
      val fields = line.split('\t')
      Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble)
    }

  def split(data: RDD[Rating]): Split[Rating]=
    val Array(train, test) = data.randomSplit(Array(0.8, 0.2))
    Split(train, test)
