
import model.DataBase
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Headers, Result}
import play.api.test.Helpers._
import play.api.test._
import utils.Utils

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

class UserSpec extends PlaySpec with OneAppPerTest with BeforeAndAfter {

  before {
    DataBase.User.clean
  }

  def emptyFunction(p: Option[Future[Result]]): Unit = ()

  def requestBody(pseudo: String, password: String) = Json.obj(
    "pseudo" -> pseudo,
    "password" -> password
  )

  "Create user" should {

    def oneTest(body: JsObject, returnCode: Int)(f: Option[Future[Result]] => Unit): Unit = {
      val result = route(app, FakeRequest(POST, "/user/create", Headers(), body))
      if (!result.map(status).contains(returnCode)) {
        println(s"Expected code $returnCode, found ${result.map(status)}")
        println(s"body: ${result map contentAsString}")
        result.map(status) mustBe Some(returnCode)
      }
      f(result)
    }

    "allow registration of a new user" in  {
      oneTest(requestBody("mario", "123456789"), OK) { result =>
        val expected = Json.obj("status" -> "success")
        result.map(contentAsJson) mustBe Some(expected)
      }
    }

    "forbid registration if username already used" in  {
      val body = requestBody("mario", "123456789")
      oneTest(body, OK) (emptyFunction)
      oneTest(body, BAD_REQUEST) (emptyFunction)
    }

    "forbid registration if password or pseudo under 4 characters" in  {
      val body = Json.obj(
        "pseudo" -> "mar",
        "password" -> "123456789"
      )
      oneTest(body, BAD_REQUEST) (emptyFunction)
      val body2 = Json.obj(
        "pseudo" -> "mario",
        "password" -> "123"
      )
      oneTest(body2, BAD_REQUEST) (emptyFunction)
    }

    "return a bad request if request is poorly formatted" in {
      val body = Json.obj(
        "pseudo" -> "mar"
      )
      oneTest(body, BAD_REQUEST) (emptyFunction)
      val body2 = Json.obj(
        "password" -> "maraskdlfj"
      )
      oneTest(body2, BAD_REQUEST) (emptyFunction)
      val body3 = Json.obj(
        "klfaqew" -> "maraskdlfj"
      )
      oneTest(body3, BAD_REQUEST) (emptyFunction)
    }

    // TODO add the ability to change password...but not in the spec !

  }

  "User login" should {

    def oneTest(
      body: JsObject,
      returnCode: Int,
      user: (String, String) = ("John", "123456")
    )(f: Option[Future[Result]] => Unit): Unit = {
      Try(DataBase.User.createUser(user._1, user._2)) recover { case NonFatal(err) =>
        println(s"Error creating user ${user._1}: ${err.getMessage}")
      }
      val result = route(app, FakeRequest(POST, "/user/connect", Headers(), body))
      if (!result.map(status).contains(returnCode)) {
        println(s"Expected code $returnCode, found ${result.map(status)}")
        println(s"body: ${result map contentAsString}")
        result.map(status) mustBe Some(returnCode)
      }
      f(result)
    }

    "succeed if the login and password are correct" in  {
      oneTest(requestBody("John", "123456"), OK) (emptyFunction)
    }

    "fail if the user is not registered" in  {
      oneTest(requestBody("Marcel", "123456"), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains("Unknow user ")) mustBe Some(true)
      }
    }

    // TODO add base64 encoding of pseudo and password
//    "fail if login or password are not encoded in base64" in  {
//      ???
//    }

    "fail if the password is not correct" in  {
      oneTest(requestBody("John", "1234567"), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains(Utils.invalidPassword)) mustBe Some(true)
      }
    }

  }

}
