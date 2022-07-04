package domains.movielens.algorithms

import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.rdd.RDD

import traits.Trainable

final class MovieRecommenderV1(
  rank: Int = 50,
  iterations: Int = 100,
  lambda: Double = 0.1
) extends Trainable[Rating, MatrixFactorizationModel]:

  def train(data: RDD[Rating]): MatrixFactorizationModel =
    ALS.train(data, rank, iterations, lambda)
