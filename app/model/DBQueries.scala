package model

import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.github.mauricio.async.db.{Connection, QueryResult}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import utils.ControllerUtils.ElectricityManagerError

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object DBQueries {
  val conf = ConfigFactory.load()
  val user = conf.getString("postgres.user")
  val password = conf.getString("postgres.password")
  val port = conf.getString("postgres.port")
  val databaseName = conf.getString("postgres.databaseName")
  val connectionUrl = s"jdbc:postgresql://localhost:$port/$databaseName?username=$user&password=$password"

  private val factory = new PostgreSQLConnectionFactory(URLParser.parse(connectionUrl))
  // TODO add deconnection
  val pool = new ConnectionPool(factory, PoolConfiguration.Default)

  def createConnection: Connection = {
    val configuration = URLParser.parse(connectionUrl)
    val connect: Connection = new PostgreSQLConnection(configuration)
    Await.result(connect.connect, Duration(5, "s"))
    connect
  }

  def eitherToFuture[B](either: Either[String, B]): Future[B] = either match {
    case Right(a) => Future.successful(a)
    case Left(b) => Future.failed(ElectricityManagerError(b))
  }

  def cleanDB: Future[QueryResult] = pool.sendQuery(
    s"truncate ${User.tableName}, ${PowerStation.tableName}, ${PowerVariation.tableName} CASCADE"
  )

  object User {
    val tableName = "utilizer"

    def alreadyExist(name: String): Future[Boolean] = pool.sendQuery(
      s"SELECT id from $tableName WHERE $tableName.pseudo = '$name'"
    ) map { result =>
      val rows = result.rows
      rows forall (_.nonEmpty)
    }

    def createUser(pseudo: String, password: String): Future[QueryResult] =
    pool.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (pseudo, password)
         | VALUES (?, ?)
         |""".stripMargin, Seq(pseudo, BCrypt.hashpw(password, BCrypt.gensalt()))
    )

    def getUserByPseudo(pseudo: String): Future[User] = pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE pseudo = ?", Seq(pseudo)
    ) flatMap { model.User.fromDbResult(_, pseudo) match {
      case Left(errorMessage) => Future.failed(ElectricityManagerError(errorMessage))
      case Right(userObj) => Future.successful(userObj)
    }}

    def clean: Future[QueryResult] = pool.sendQuery(s"truncate $tableName CASCADE")
  }

  object PowerStation {
    val tableName = "power_station"

    def create(
      typePW: String,
      code: String,
      maxCapacity: Int,
      proprietary: User
    ): Future[QueryResult] = pool.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (type, code, max_capacity, proprietary)
         | VALUES (?, ?, ?, ?)
         |""".stripMargin, Seq(typePW, code, maxCapacity, proprietary.id)
    )

    private def getIncompleteById(
      id: Int,
      user: User
    ): Future[PowerStation] = pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE id = ?", Seq(id)
    ) flatMap { queryResult =>
      val rows = queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
      if (rows.nonEmpty) Future.successful(rows.head)
      else Future.failed(ElectricityManagerError(s"No power station with id $id"))
    } flatMap { row => model.PowerStation.fromDbResult(row, user) match {
        case Left(errorMessage) => Future.failed(ElectricityManagerError(errorMessage))
        case Right(powerStation) => Future.successful(powerStation)
    }}

    def withAssociatedVariation(
      powerStation: PowerStation
    ): Future[PowerStation] = for {
      eitherVariations <- DBQueries.PowerVariation.getAllAssiociatedWithPowerStation(powerStation)
      // TODO what to do with variations which are not correct ?
      variations <- Future.successful(
        eitherVariations.filter(_.isRight).map(_.right.get)
      )
    } yield powerStation.copy(
      variations = variations,
      currentEnergy = variations.foldLeft(0)(_ + _.delta)
    )

    def allOwnedByUser(user: User): Future[Seq[Future[PowerStation]]] = pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE proprietary = ?", Seq(user.id)
    ) map { queryResult =>
      queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
    } map { _.map { row => for {
      incompletePS <- eitherToFuture(model.PowerStation.fromDbResult(row, user))
      completePS <- withAssociatedVariation(incompletePS)
    } yield completePS }}

    def getById(id: Int, user: User): Future[PowerStation] =
      DBQueries.PowerStation.getIncompleteById(id, user) flatMap withAssociatedVariation

  }

  object PowerVariation {
    val tableName = "electricity_variation"

    def getAllAssiociatedWithPowerStation(
      ps: PowerStation
    ): Future[Seq[Either[String, PowerVariation]]] = pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE station = ?", Seq(ps.id)
    ) map { queryResult =>
      queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
    } map { rows => rows.map { row => model.PowerVariation.fromDbResult(row, ps) } }

    def create(powerStation: PowerStation, delta: Int): Future[QueryResult] = pool.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (execution_date, delta, station)
         | VALUES (?, ?, ?)
         |""".stripMargin, Seq(DateTime.now, delta, powerStation.id)
    )
  }

}
