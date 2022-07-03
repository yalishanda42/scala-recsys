import org.apache.spark.mllib.recommendation.Rating

object Utils:
  def stringToRatingMapper(line: String): Rating =
    line.split('\t') match
      case Array(user, item, rate, _) => Rating(user.toInt, item.toInt, rate.toDouble)