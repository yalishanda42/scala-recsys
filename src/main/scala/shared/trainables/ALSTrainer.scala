package shared.trainables

import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.rdd.RDD

import traits.Trainable

final case class ALSTrainer(
  rank: Int = 50,
  iterations: Int = 20,
  regularizationCoeff: Double = 0.1
) extends Trainable[Rating, MatrixFactorizationModel]:
  def train(data: RDD[Rating]): MatrixFactorizationModel =
    ALS.train(data, rank, iterations, regularizationCoeff)
