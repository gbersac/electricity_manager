package utils

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ResponseHeader, Result}

import scala.concurrent.Future

object Utils {
  val jsonMimeType = "application/json"

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
