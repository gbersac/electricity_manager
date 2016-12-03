package model

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.github.mauricio.async.db.{Connection, QueryResult}
import com.typesafe.config.ConfigFactory
import utils.Utils

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object DataBase {
  val conf = ConfigFactory.load()
  val user = conf.getString("postgres.user")
  val password = conf.getString("postgres.password")
  val port = conf.getString("postgres.port")
  val databaseName = conf.getString("postgres.databaseName")
  val connectionUrl = s"jdbc:postgresql://localhost:$port/$databaseName?username=$user&password=$password"

  implicit val connection = {
    val configuration = URLParser.parse(connectionUrl)
    val connect: Connection = new PostgreSQLConnection(configuration)
    Await.result(connect.connect, 5 seconds)
    connect
  }

  def cleanDB: Future[QueryResult] = connection.sendQuery(
    s"truncate ${User.tableName}, ${PowerStation.tableName} CASCADE"
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
      case Left(errorMessage) => Future.failed(Utils.ElectricityManagerError(errorMessage))
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

    def clean: Future[QueryResult] = connection.sendQuery(s"truncate $tableName CASCADE")
  }

  // TODO add deconnection
  // connection.disconnect

}
