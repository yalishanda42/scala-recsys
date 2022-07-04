package domains.movielens.algorithms

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import traits.Algorithm
import domains.movielens.datatransformers.MovieLensTransformerV1
import domains.movielens.trainables.ALSTrainer
import shared.testables.MatrixFactorizationModelTester

case class MovieRecommenderV1() extends Algorithm[Rating, MatrixFactorizationModel]:
  lazy val transformer = MovieLensTransformerV1()
  lazy val trainer = ALSTrainer()
  lazy val tester = MatrixFactorizationModelTester()
