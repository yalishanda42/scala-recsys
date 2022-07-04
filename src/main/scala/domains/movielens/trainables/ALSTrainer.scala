package domains.movielens.trainables

import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.rdd.RDD

import traits.Trainable

final case class ALSTrainer() extends Trainable[Rating, MatrixFactorizationModel]:
  def train(data: RDD[Rating]): MatrixFactorizationModel =
    ALS.train(data, 50, 20, 0.1)
