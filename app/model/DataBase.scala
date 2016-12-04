package model

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.github.mauricio.async.db.{Connection, QueryResult}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import utils.Utils.ElectricityManagerError

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object DataBase {
  val conf = ConfigFactory.load()
  val user = conf.getString("postgres.user")
  val password = conf.getString("postgres.password")
  val port = conf.getString("postgres.port")
  val databaseName = conf.getString("postgres.databaseName")
  val connectionUrl = s"jdbc:postgresql://localhost:$port/$databaseName?username=$user&password=$password"

  def createConnection: Connection = {
    val configuration = URLParser.parse(connectionUrl)
    val connect: Connection = new PostgreSQLConnection(configuration)
    Await.result(connect.connect, Duration(5, "s"))
    connect
  }

  implicit val connection = createConnection

  def eitherToFuture[B](either: Either[String, B]): Future[B] = either match {
    case Right(a) => Future.successful(a)
    case Left(b) => Future.failed(ElectricityManagerError(b))
  }

  def cleanDB: Future[QueryResult] = connection.sendQuery(
    s"truncate ${User.tableName}, ${PowerStation.tableName}, ${PowerVariation.tableName} CASCADE"
  )

  object User {
    val tableName = "utilizer"

    def alreadyExist(name: String): Future[Boolean] = connection.sendQuery(
      s"SELECT id from $tableName WHERE $tableName.pseudo = '$name'"
    ) map { result =>
      val rows = result.rows
      rows forall (_.nonEmpty)
    }

    def createUser(pseudo: String, password: String): Future[QueryResult] = connection.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (pseudo, password)
         | VALUES (?, ?)
         |""".stripMargin, Seq(pseudo, password)
    )

    def getUserByPseudo(pseudo: String): Future[User] = connection.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE pseudo = ?", Seq(pseudo)
    ) flatMap { model.User.fromDbResult(_, pseudo) match {
      case Left(errorMessage) => Future.failed(ElectricityManagerError(errorMessage))
      case Right(userObj) => Future.successful(userObj)
    }}

    def clean: Future[QueryResult] = connection.sendQuery(s"truncate $tableName CASCADE")
  }

  object PowerStation {
    val tableName = "power_station"

    def create(
      typePW: String,
      code: String,
      maxCapacity: Int,
      proprietary: User
    ): Future[QueryResult] = connection.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (type, code, max_capacity, proprietary)
         | VALUES (?, ?, ?, ?)
         |""".stripMargin, Seq(typePW, code, maxCapacity, proprietary.id)
    )

    def getById(id: Int, user: User): Future[PowerStation] = connection.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE id = ?", Seq(id)
    ) flatMap { queryResult =>
      val rows = queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
      if (rows.nonEmpty) Future.successful(rows.head)
      else Future.failed(ElectricityManagerError(s"No power station with id $id"))
    } flatMap { row => model.PowerStation.fromDbResult(row, user) match {
        case Left(errorMessage) => Future.failed(ElectricityManagerError(errorMessage))
        case Right(powerStation) => Future.successful(powerStation)
    }}

    def allOwnedByUser(user: User): Future[Seq[Future[PowerStation]]] = connection.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE proprietary = ?", Seq(user.id)
    ) map { queryResult =>
      queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
    } map { _.map { row => for {
      psEither <- eitherToFuture(model.PowerStation.fromDbResult(row, user))
      completePS <- psEither.withAssociatedVariation
    } yield completePS }}

  }

  object PowerVariation {
    val tableName = "electricity_variation"

    def getAllAssiociatedWithPowerStation(
      ps: PowerStation
    ): Future[Seq[Either[String, PowerVariation]]] = connection.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE station = ?", Seq(ps.id)
    ) map { queryResult =>
      queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
    } map { rows => rows.map { row => model.PowerVariation.fromDbResult(row, ps) } }

    def create(powerStation: PowerStation, delta: Int): Future[QueryResult] = connection.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (execution_date, delta, station)
         | VALUES (?, ?, ?)
         |""".stripMargin, Seq(DateTime.now, delta, powerStation.id)
    )
  }

  // TODO add deconnection
  // connection.disconnect

}
