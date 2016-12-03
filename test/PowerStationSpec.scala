import controllers.PowerStationController
import model.DataBase
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Headers, Result}
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PowerStationSpec extends PlaySpec with OneAppPerTest with BeforeAndAfter {

  before {
    Await.ready(DataBase.cleanDB, Duration(5, "s"))
    Await.ready(DataBase.User.createUser("John", "123456"), Duration(5, "s"))
  }

  implicit val ec = scala.concurrent.ExecutionContext.global

  def emptyFunction(p: Option[Future[Result]]): Unit = ()

  "Power station declaration" should {

    def requestBody(
      typePW: String,
      code: String,
      maxCapacity: Int,
      pseudo: String = "John"
    ): JsValue = Json.obj(
      "pseudo" -> pseudo,
      "password" -> "123456",
      "typePW" -> typePW,
      "code" -> code,
      "maxCapacity" -> maxCapacity
    )

    def oneTest(
      body: JsValue,
      expectedHttpReturnCode: Int
    )(f: Option[Future[Result]] => Unit): Unit = {
      val result = route(app, FakeRequest(POST, "/power_station/create", Headers(), body))
      if (!result.map(status).contains(expectedHttpReturnCode)) {
        println(s"Expected code $expectedHttpReturnCode, found ${result.map(status)}")
        println(s"body: ${result map contentAsString}")
        result.map(status) mustBe Some(expectedHttpReturnCode)
      }
      f(result)
    }

    "succeed if it declares a Power station with capacity, code and type" in  {
      oneTest(requestBody("solar panel", "SP1", 100), OK) (emptyFunction)
    }

    "fail if capacity of power station is negative or zero" in  {
      oneTest(requestBody("solar panel", "SP1", 0), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains(PowerStationController.incorrectCapacityError)) mustBe
          Some(true)
      }
      oneTest(requestBody("solar panel", "SP1", -10), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains(PowerStationController.incorrectCapacityError)) mustBe
          Some(true)
      }
    }

    "fail if user does not exist" in {
      oneTest(requestBody("solar panel", "SP1", 100, "Unknow"), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains("Unknow user ")) mustBe Some(true)
      }
    }

    "fail if information are missing" in {
      val body = Json.obj(
        "pseudo" -> "John",
        "password" -> "123456",
        "code" -> "code",
        "max_capacity" -> 100
      )
      oneTest(body, BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains(PowerStationController.missingPowerStationInfosError)) mustBe
          Some(true)
      }
    }
  }

  "Power station usage" should {

    "succeed if new energy stock of power station is between O and max capacity" in {
      ???
    }

    "fail if new energy stock of power station is under O or > max capacity" in {
      ???
    }

    "fail if power station doesn't exist" in {
      ???
    }

  }

  "Stock consultation" should {

    "return an history of all the energy stock changes" in {
      ???
    }

  }

}
