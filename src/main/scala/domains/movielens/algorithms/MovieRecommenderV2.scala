package domains.movielens.algorithms

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import traits.Algorithm
import domains.movielens.datatransformers.MovieLensTransformer
import domains.movielens.trainables.ALSTrainer
import shared.testables.MatrixFactorizationModelTester

case class MovieRecommenderV2() extends Algorithm[Rating, MatrixFactorizationModel]:
  lazy val transformer = MovieLensTransformer()
  lazy val trainer = ALSTrainer(20, 10, 0.05)
  lazy val tester = MatrixFactorizationModelTester()
