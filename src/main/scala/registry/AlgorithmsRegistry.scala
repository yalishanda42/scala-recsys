package registry

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import cats.implicits.*

import domains.movielens.algorithms.*
import domains.restaurants.algorithms.*
import domains.books.algorithms.*
import traits.Algorithm


object AlgorithmsRegistry:
  def apply(name: String): Option[Algorithm[Rating, MatrixFactorizationModel]] = name match
    case "movielens-v1" => MovieRecommenderV1().some
    case "movielens-v2" => MovieRecommenderV2().some
    case "movielens-v3" => MovieRecommenderV3().some
    case "books-v1" => BooksRecommenderV1().some
    case "restaurants-v1" => RestaurantsRecommenderV1().some
    case _ => None
