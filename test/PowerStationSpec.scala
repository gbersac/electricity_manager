import com.github.mauricio.async.db.QueryResult
import controllers.PowerStationController
import model.{DataBase, PowerStation, User}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.AsyncAssertions
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Headers, Result}
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class PowerStationSpec extends PlaySpec with OneAppPerTest with BeforeAndAfter with AsyncAssertions {

  val userJohn = User(1, "John", "123456")
  val userMarc = User(2, "Marc", "123456")

  def createUser(user: User): Future[QueryResult] = DataBase.connection.sendPreparedStatement(
    s"""
       | INSERT INTO utilizer (id, pseudo, password)
       | VALUES (?, ?, ?)
       |""".stripMargin, Seq(user.id, user.pseudo, user.password)
  )


  before {
    Await.ready(DataBase.cleanDB, Duration(5, "s"))
    Await.ready(createUser(userJohn), Duration(5, "s"))
    Await.ready(createUser(userMarc), Duration(5, "s"))
  }

  implicit val ec = scala.concurrent.ExecutionContext.global

  def emptyFunction(p: Option[Future[Result]]): Unit = ()

  def requestBodyCreatePowerStation(
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

  def withPowerStation(
    id: Int,
    typePW: String,
    code: String,
    maxCapacity: Int,
    user: User = userJohn
  )(f: PowerStation => Unit): Unit = {
    val future = DataBase.connection.sendPreparedStatement(
      s"""
         | INSERT INTO ${DataBase.PowerStation.tableName} (id, type, code, max_capacity, proprietary)
         | VALUES (?, ?, ?, ?, ?)
         |""".stripMargin, Seq(id, typePW, code, maxCapacity, user.id)
    ) map { queryResult =>
      if (queryResult.rowsAffected != 1)
        throw new RuntimeException("Error creating power station")
      else {
        val powerStation = PowerStation(id, typePW, code, maxCapacity, user, Seq(), 0)
        f(powerStation)
      }
    }
    val w = new Waiter
    future.onComplete {
      case Failure(e) => throw e
      case Success(_) => w.dismiss()
    }
    w.await
  }

  def doesResultContain(expected: String)(result: Option[Future[Result]]): Unit = {
    val contain = result.map(contentAsString(_).contains(expected)) == Some(true)
    if (!contain) {
      println(s"body: ${result map contentAsString}")
      assert(false)
    }
  }

  "Power station declaration" should {

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
      oneTest(requestBodyCreatePowerStation("solar panel", "SP1", 100), OK) (emptyFunction)
    }

    "fail if capacity of power station is negative or zero" in  {
      oneTest(requestBodyCreatePowerStation("solar panel", "SP1", 0), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains(PowerStationController.incorrectCapacityError)) mustBe
          Some(true)
      }
      oneTest(requestBodyCreatePowerStation("solar panel", "SP1", -10), BAD_REQUEST) { result =>
        result.map(contentAsString(_).contains(PowerStationController.incorrectCapacityError)) mustBe
          Some(true)
      }
    }

    "fail if user does not exist" in {
      oneTest(requestBodyCreatePowerStation("solar panel", "SP1", 100, "Unknow"), BAD_REQUEST) { result =>
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

    def addPowerVariation(
      delta: Int,
      powerStation: PowerStation,
      expectedHttpReturnCode: Int = OK,
      user: User = userJohn
    )(f: Option[Future[Result]] => Unit): Unit = {
      val body = Json.obj(
        "delta" -> delta,
        "stationId" -> powerStation.id,
        "pseudo" -> user.pseudo,
        "password" -> user.password
      )
      val result = route(app, FakeRequest(POST, "/power_station/use", Headers(), body))
      if (!result.map(status).contains(expectedHttpReturnCode)) {
        println(s"result body: ${result map contentAsString}")
        println(s"Expected code $expectedHttpReturnCode, found ${result.map(status)}")
        result.map(status) mustBe Some(expectedHttpReturnCode)
      }
      result.map(Await.ready(_, Duration(5, "s")))
      f(result)
    }

    "succeed if new energy stock of power station is between O and max capacity" in {
      withPowerStation(1, "solar panel", "SP0", 100) { powerStation =>
        addPowerVariation(100, powerStation) (emptyFunction)
        addPowerVariation(-100, powerStation) (emptyFunction)
      }
    }

    "fail if new energy stock of power station is under O or > max capacity" in {
      def resultIncorrectDelta(result: Option[Future[Result]]): Unit =
        doesResultContain("Incorrect delta, new energy level can't be over")(result)

      withPowerStation(1, "solar panel", "SP1", 100) { powerStation =>
        addPowerVariation(-1, powerStation, BAD_REQUEST) (resultIncorrectDelta)
        addPowerVariation(200, powerStation, BAD_REQUEST) (resultIncorrectDelta)
        addPowerVariation(50, powerStation) (emptyFunction)
        addPowerVariation(51, powerStation, BAD_REQUEST) (resultIncorrectDelta)
      }
    }

    "fail if power station doesn't exist" in {
      addPowerVariation(
        -1,
        PowerStation(42, "", "", 100, User(42, "asdf", "asdf"), Seq(), 0),
        BAD_REQUEST
      ) (doesResultContain("No power station with id 42"))
    }

    "fail if the power station doesn't belong to logged in user" in {
      withPowerStation(1, "solar panel", "SP1", 100, userMarc) { powerStation =>
        addPowerVariation(
          51, powerStation, BAD_REQUEST, userJohn
        ) (
          doesResultContain(PowerStation.noPermissionError)
        )
      }
    }

//    "to delete" in {
//      withPowerStation(1, "solar panel", "SP1", 100, userMarc) { powerStation =>
//        assert(false)
//      }
//    }

  }

//  "Stock consultation" should {
//
//    "return an history of all the energy stock changes" in {
//      ???
//    }
//
//  }

}
