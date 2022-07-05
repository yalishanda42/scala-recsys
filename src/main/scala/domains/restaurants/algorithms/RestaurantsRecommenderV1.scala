package domains.restaurants.algorithms

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import traits.Algorithm
import domains.restaurants.datatransformers.RestaurantsTransformer
import shared.trainables.ALSTrainer
import shared.testables.MatrixFactorizationModelTester

case class RestaurantsRecommenderV1() extends Algorithm[Rating, MatrixFactorizationModel]:
  lazy val transformer = RestaurantsTransformer()
  lazy val trainer = ALSTrainer()
  lazy val tester = MatrixFactorizationModelTester()
