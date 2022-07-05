package registry

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}

import domains.movielens.algorithms.*
import domains.restaurants.algorithms.*
import domains.books.algorithms.*
import traits.Algorithm


object AlgorithmsRegistry:
  def apply(name: String): Algorithm[Rating, MatrixFactorizationModel] = name match
    case "movielens-v1" => MovieRecommenderV1()
    case "movielens-v2" => MovieRecommenderV2()
    case "movielens-v3" => MovieRecommenderV3()
    case "books-v1" => BooksRecommenderV1()
    case "restaurants-v1" => RestaurantsRecommenderV1()
