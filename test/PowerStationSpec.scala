import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

class PowerStationSpec extends PlaySpec with OneAppPerTest {

  "Power station declaration" should {

    "succeed if it declares a Power station with capacity and type" in  {
      ???
    }

    "fail if capacity of power station is negative" in  {
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
