package model

import com.github.mauricio.async.db.QueryResult

import scala.util.{Failure, Success, Try}

case class User(
  id: Int,
  pseudo: String,
  password: String
) {
  // TODO improve login method
  def isPasswordCorrect(input: String): Boolean = password == input
}

object User {
  def fromDbResult(
    result: QueryResult,
    pseudo: String
  ): Either[String, User] = result.rows.flatMap(_.headOption) map { row => Try(User(
    id = row("id").asInstanceOf[Int],
    pseudo = pseudo,
    password = row("password").asInstanceOf[String]
  ))} match {
    case Some(Success(user)) => Right(user)
    case Some(Failure(err)) => Left(s"Malformed db row: ${err.getMessage}")
    case None => Left(s"Unknow user $pseudo")
  }
}
