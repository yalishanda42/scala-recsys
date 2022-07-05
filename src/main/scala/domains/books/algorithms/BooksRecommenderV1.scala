package domains.books.algorithms

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import traits.Algorithm
import domains.books.datatransformers.BooksTransformer
import shared.trainables.ALSTrainer
import shared.testables.MatrixFactorizationModelTester

case class BooksRecommenderV1() extends Algorithm[Rating, MatrixFactorizationModel]:
  lazy val transformer = BooksTransformer()
  lazy val trainer = ALSTrainer()
  lazy val tester = MatrixFactorizationModelTester()
