package domains.movielens.algorithms

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import traits.Algorithm
import domains.movielens.datatransformers.MovieLensTransformer
import shared.trainables.HyperALSTrainer
import shared.testables.MatrixFactorizationModelTester
import metrics.RMSE

case class MovieRecommenderV3() extends Algorithm[Rating, MatrixFactorizationModel]:
  lazy val transformer = MovieLensTransformer()
  lazy val trainer = HyperALSTrainer(tester, RMSE(), Seq(10, 20, 25), Seq(5, 10, 20), Seq(0.01, 0.05, 0.1))
  lazy val tester = MatrixFactorizationModelTester()
