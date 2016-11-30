import org.scalatestplus.play._
import play.api.mvc.Headers
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{JsNull,Json,JsString,JsValue}

class PowerStationSpec extends PlaySpec with OneAppPerTest {

  "Power station declaration" should {

//    def oneTest()

    "succeed if it declares a Power station with capacity and type" in  {
//      val body = Json.obj(
//        "login" -> Json.obj(
//
//        ),
//        "type" -> "solar panel",
//        "code" -> "SP1",
//        "max_capacity" -> 100
//      )
//      route(app, FakeRequest(POST, "/power_station/create", Headers(), body)).map(status) mustBe Some(NOT_FOUND)
      ???
    }

    "fail if capacity of power station is negative or zero" in  {
      ???
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
