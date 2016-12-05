package model

import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import utils.ControllerUtils.ElectricityManagerError
import utils.DBConnectionPool

import scala.concurrent.Future

object DBQueries {

  def eitherToFuture[B](either: Either[String, B]): Future[B] = either match {
    case Right(a) => Future.successful(a)
    case Left(b) => Future.failed(ElectricityManagerError(b))
  }

  object User {
    val tableName = "utilizer"

    def alreadyExist(name: String)(implicit db: DBConnectionPool): Future[Boolean] = db.pool.sendQuery(
      s"SELECT id from $tableName WHERE $tableName.pseudo = '$name'"
    ) map { result =>
      val rows = result.rows
      rows forall (_.nonEmpty)
    }

    def createUser(pseudo: String, password: String)(implicit db: DBConnectionPool): Future[QueryResult] =
    db.pool.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (pseudo, password)
         | VALUES (?, ?)
         |""".stripMargin, Seq(pseudo, BCrypt.hashpw(password, BCrypt.gensalt()))
    )

    def getUserByPseudo(pseudo: String)(implicit db: DBConnectionPool): Future[User] = db.pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE pseudo = ?", Seq(pseudo)
    ) flatMap { model.User.fromDbResult(_, pseudo) match {
      case Left(errorMessage) => Future.failed(ElectricityManagerError(errorMessage))
      case Right(userObj) => Future.successful(userObj)
    }}

    def clean(implicit db: DBConnectionPool): Future[QueryResult] = db.pool.sendQuery(s"truncate $tableName CASCADE")

  }

  object PowerStation {
    val tableName = "power_station"

    def create(
      typePW: String,
      code: String,
      maxCapacity: Int,
      proprietary: User
    )(implicit db: DBConnectionPool): Future[QueryResult] = db.pool.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (type, code, max_capacity, proprietary)
         | VALUES (?, ?, ?, ?)
         |""".stripMargin, Seq(typePW, code, maxCapacity, proprietary.id)
    )

    private def getIncompleteById(
      id: Int,
      user: User
    )(implicit db: DBConnectionPool): Future[PowerStation] = db.pool.sendPreparedStatement(
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
    )(implicit db: DBConnectionPool): Future[PowerStation] = for {
      eitherVariations <- PowerVariation.getAllAssiociatedWithPowerStation(powerStation)
      // TODO what to do with variations which are not correct ?
      variations <- Future.successful(
        eitherVariations.filter(_.isRight).map(_.right.get)
      )
    } yield powerStation.copy(
      variations = variations,
      currentEnergy = variations.foldLeft(0)(_ + _.delta)
    )

    def allOwnedByUser(
      user: User
    )(
      implicit db: DBConnectionPool
    ): Future[Seq[Future[PowerStation]]] = db.pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE proprietary = ?", Seq(user.id)
    ) map { queryResult =>
      queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
    } map { _.map { row => for {
      incompletePS <- eitherToFuture(model.PowerStation.fromDbResult(row, user))
      completePS <- withAssociatedVariation(incompletePS)
    } yield completePS }}

    def getById(id: Int, user: User)(implicit db: DBConnectionPool): Future[PowerStation] =
      PowerStation.getIncompleteById(id, user) flatMap withAssociatedVariation

  }

  object PowerVariation {
    val tableName = "electricity_variation"

    def getAllAssiociatedWithPowerStation(
      ps: PowerStation
    )(
      implicit db: DBConnectionPool
    ): Future[Seq[Either[String, PowerVariation]]] = db.pool.sendPreparedStatement(
      s"SELECT * FROM $tableName WHERE station = ?", Seq(ps.id)
    ) map { queryResult =>
      queryResult.rows.map(_.iterator.toSeq).getOrElse(Seq())
    } map { rows => rows.map { row => model.PowerVariation.fromDbResult(row, ps) } }

    def create(
      powerStation: PowerStation,
      delta: Int
    )(implicit db: DBConnectionPool): Future[QueryResult] = db.pool.sendPreparedStatement(
      s"""
         | INSERT INTO $tableName (execution_date, delta, station)
         | VALUES (?, ?, ?)
         |""".stripMargin, Seq(DateTime.now, delta, powerStation.id)
    )

  }

}
