package shared.trainables

import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.rdd.RDD
import cats._, cats.data._, cats.implicits._ // for .tupled

import traits.{Trainable, Testable}
import metrics.Metric

case class HyperALSTrainer(
  tester: Testable[Rating, MatrixFactorizationModel],
  metric: Metric,
  ranks: Seq[Int],
  maxIterations: Seq[Int],
  regParams: Seq[Double]
) extends Trainable[Rating, MatrixFactorizationModel]:
  def train(data: RDD[Rating]): MatrixFactorizationModel =
    val Array(trainSet, validationSet) = data.randomSplit(Array(0.9, 0.1))
    (ranks, maxIterations, regParams).tupled.map {
      case (rank, maxIter, regParam) =>
        val model = ALS.train(trainSet, rank, maxIter, regParam)
        (model, tester.test(model, metric, validationSet))
    }.sortBy(_._2).head._1
