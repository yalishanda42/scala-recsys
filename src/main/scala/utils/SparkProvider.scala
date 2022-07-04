package utils

import cats.effect.{Resource, IO}
import org.apache.spark.{SparkConf, SparkContext}

object SparkProvider:
  def conf(name: String, master: Option[String] = None) = new SparkConf()
    .setAppName(name)
    .setMaster(master.getOrElse("local[*]"))

  def sparkContext(sparkConf: SparkConf): Resource[IO, SparkContext] =
    Resource.make(
      IO(SparkContext.getOrCreate(sparkConf))
    )(sc =>
      IO(sc.stop())
    )

  def sparkContext(name: String, master: Option[String] = None): Resource[IO, SparkContext] =
    sparkContext(conf(name, master))
