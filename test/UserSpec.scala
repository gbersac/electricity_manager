import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

class UserSpec extends PlaySpec with OneAppPerTest {

  "User registration" should {

    "allow registration of a new user" in  {
      ???
    }

    "forbid registration if username already used" in  {
      ???
    }

    "forbid registration if password under 4 characters" in  {
      ???
    }

    // TODO add the ability to change password...but not in the spec !

  }

  "User login" should {

    "succeed if the login and password are correct and encode in base64" in  {
      ???
    }

    "fail if login or password are not encoded in base64" in  {
      ???
    }

    "fail if login or password are not correct" in  {
      ???
    }

  }

}
