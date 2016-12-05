package utils

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ResponseHeader, Result}

import scala.concurrent.Future

object ControllerUtils {
  val jsonMimeType = "application/json"
  val unexpectedError = "Unexpected error"

  case class ElectricityManagerError(msg: String) extends Exception

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
