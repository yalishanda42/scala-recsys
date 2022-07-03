import cats.effect.*
import org.apache.spark.{SparkConf, SparkContext}

object SparkProvider:
  def conf(name: String) = new SparkConf().setAppName(name)

  def sparkContext(sparkConf: SparkConf): Resource[IO, SparkContext] =
    Resource.make(IO(SparkContext.getOrCreate(sparkConf)))(sc => IO(sc.stop()))

  def sparkContext(name: String): Resource[IO, SparkContext] =
    sparkContext(conf(name))
