package utils

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.mauricio.async.db.{RowData, QueryResult, Connection}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object DataBase {

  implicit val connection = {
    val configuration = URLParser.parse("jdbc:postgresql://localhost:5432/world?username=postgres&password=root")
    val connect: Connection = new PostgreSQLConnection(configuration)
    Await.result(connect.connect, 5 seconds)
    connect
  }

  def getCities(): Future[String] = {
    connection.sendQuery("SELECT * FROM city") map { queryResult =>
      queryResult.rows.get(0)("name").asInstanceOf[String]
    }
  }

  // TODO add deisconnection
  // connection.disconnect

}
