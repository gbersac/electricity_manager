package model

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.typesafe.config.ConfigFactory

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

  def getCities: Future[String] = {
    connection.sendQuery("SELECT * FROM city") map { queryResult =>
      queryResult.rows.get(0)("name").asInstanceOf[String]
    }
  }

  // TODO add deisconnection
  // connection.disconnect

}
