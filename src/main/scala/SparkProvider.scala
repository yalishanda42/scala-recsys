import cats.effect._
import org.apache.spark.{SparkConf, SparkContext}

object SparkProvider {
  def conf(name: String) = new SparkConf().setAppName(name)

  def sparkContext(sparkConf: SparkConf): Resource[SparkContext] =
    Resource.make(SparkContext.getOrCreate(sparkConf))(sc => sc.stop())
}