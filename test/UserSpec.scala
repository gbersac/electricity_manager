
import model.DataBase
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Headers
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Result

import scala.concurrent.Future

class UserSpec extends PlaySpec with OneAppPerTest with BeforeAndAfter {

  before {
    DataBase.User.clean
  }

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

    def emptyFunction(p: Option[Future[Result]]): Unit = ()

    "allow registration of a new user" in  {
      val body = Json.obj(
        "pseudo" -> "mario",
        "password" -> "123456789"
      )
      oneTest(body, OK) { result =>
        val expected = Json.obj("status" -> "success")
        result.map(contentAsJson) mustBe Some(expected)
      }
    }

    "forbid registration if username already used" in  {
      val body = Json.obj(
        "pseudo" -> "mario",
        "password" -> "123456789"
      )
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

    "succeed if the login and password are correct" in  {
      ???
    }

    // TODO add base64 encoding of pseudo and password
//    "fail if login or password are not encoded in base64" in  {
//      ???
//    }

    "fail if login or password are not correct" in  {
      ???
    }

  }

}
