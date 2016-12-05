import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.pool.ConnectionPool
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import model.User
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future

object TestUtils {

  def createUser(
    user: User
  )(
    implicit pool: ConnectionPool[PostgreSQLConnection]
  ): Future[QueryResult] = pool.sendPreparedStatement(
    s"""
       | INSERT INTO utilizer (id, pseudo, password)
       | VALUES (?, ?, ?)
       |""".stripMargin, Seq(user.id, user.pseudo, BCrypt.hashpw(user.password, BCrypt.gensalt()))
  )

}
