package utils

import cats.effect.IO

case class Logger(prefix: String = "") {
  enum Level:
    case Info, Error

  def log(level: Level)(message: String): IO[Unit] =
    level match
      case Level.Info => IO(println(s"$prefix [INFO] $message"))
      case Level.Error => IO(System.err.println(s"$prefix [ERROR] $message"))

  def logInfo = log(Level.Info)(_)
  def logError = log(Level.Error)(_)
}
