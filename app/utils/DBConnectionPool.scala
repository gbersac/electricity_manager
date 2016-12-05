package utils

import javax.inject._

import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.typesafe.config.ConfigFactory
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class DBConnectionPool @Inject() (appLifecycle: ApplicationLifecycle) {
  val pool = DBConnectionPool.createPool

  appLifecycle.addStopHook { () =>
    pool.close
    Future.successful(())
  }

}

object DBConnectionPool {

  def createPool: ConnectionPool[PostgreSQLConnection] = {
    val conf = ConfigFactory.load()
    val user = conf.getString("postgres.user")
    val password = conf.getString("postgres.password")
    val port = conf.getString("postgres.port")
    val databaseName = conf.getString("postgres.databaseName")
    val connectionUrl = s"jdbc:postgresql://localhost:$port/$databaseName?username=$user&password=$password"

    val configuration = URLParser.parse(connectionUrl)
    val factory = new PostgreSQLConnectionFactory(configuration)
    new ConnectionPool(factory, PoolConfiguration.Default)
  }

}
