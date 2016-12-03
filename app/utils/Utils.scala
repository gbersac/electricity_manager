package utils

import akka.util.ByteString
import model.{DataBase, User}
import play.api.http.HttpEntity
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{Controller, Request, ResponseHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

object Utils extends Controller {
  val jsonMimeType = "application/json"

  val invalidPassword = "User password is not correct"
  val missingUserInfoError = "Body should contain a json object with keys pseudo and password"
  val unexpectedError = "Unexpected error"

  case class ElectricityManagerError(msg: String) extends Exception

  def executeWithLoggedUser(
    input: Request[JsValue]
  )(
    f: User => Future[Result]
  )(
    implicit ec: ExecutionContext
  ): Future[Result] = {
    val t = Try((
      (input.body \ "pseudo").as[String],
      (input.body \ "password").as[String]
    )) map { case (pseudo: String, password: String) =>
      DataBase.User.getUserByPseudo(pseudo) flatMap { user =>
        if (user.isPasswordCorrect(password))
          f(user)
        else
          failureResponse(invalidPassword, BAD_REQUEST)
      } recover { case ElectricityManagerError(errorMsg) => BadRequest(Utils.failureBody(errorMsg))}
    } recover {
      case JsResultException(_) => failureResponse(missingUserInfoError, BAD_REQUEST)
      case NonFatal(err) => failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
    }
    t.getOrElse(failureResponse(unexpectedError, INTERNAL_SERVER_ERROR))
  }

  def failureBody(errorMsg: String): JsValue = Json.obj(
    "status" -> "failure",
    "reason" -> errorMsg
  )

  def successBody: JsValue = Json.obj("status" -> "success")

  def failureResponse(errorMsg: String, httpReturnCode: Int): Future[Result] = Future.successful(Result(
    header = ResponseHeader(status = httpReturnCode),
    body = HttpEntity.Strict(
      data = ByteString(failureBody(errorMsg).toString),
      contentType = Option(jsonMimeType)
    )
  ))
}
