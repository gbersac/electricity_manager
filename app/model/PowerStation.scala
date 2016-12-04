package model

import com.github.mauricio.async.db.RowData
import play.api.libs.json._
import utils.ControllerUtils.ElectricityManagerError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class PowerStation(
  id: Int,
  typePW: String,
  code: String,
  maxCapacity: Int,
  proprietary: User,
  variations: Seq[PowerVariation],
  currentEnergy: Int
) {

  def withAssociatedVariation(implicit ec: ExecutionContext): Future[PowerStation] = for {
    eitherVariations <- DataBase.PowerVariation.getAllAssiociatedWithPowerStation(this)
    // TODO what to do with variations which are not correct ?
    variations <- Future.successful(
      eitherVariations.filter(_.isRight).map(_.right.get)
    )
  } yield this.copy(
    variations = variations,
    currentEnergy = variations.foldLeft(0)(_ + _.delta)
  )

  def toJson: JsValue = Json.obj(
    "id" -> id,
    "typePW" -> typePW,
    "code" -> code,
    "maxCapacity" -> maxCapacity,
    "variations" -> variations.map(_.toJson),
    "currentEnergy" -> currentEnergy
  )

}

object PowerStation {
  val noPermissionError = "You don't have permission to use this power station"

  /**
    * Return a `PowerStation` without the associated `variations` and `currentEnergy`.
    */
  def fromDbResult(
    row: RowData,
    user: User
  ): Either[String, PowerStation] = {
    if (row("proprietary").asInstanceOf[Int] == user.id) Try(PowerStation(
      id = row("id").asInstanceOf[Int],
      typePW = row("type").asInstanceOf[String],
      code = row("code").asInstanceOf[String],
      maxCapacity = row("max_capacity").asInstanceOf[Int],
      proprietary = user,
      variations = Seq(), // not correct value
      currentEnergy = 0 // not correct value
    )) else Try(throw ElectricityManagerError(noPermissionError))
  } match {
    case Success(ps) => Right(ps)
    case Failure(ElectricityManagerError(errorMsg)) => Left(errorMsg)
    case Failure(err) => Left(s"Malformed db row: ${err.getMessage}")
  }

  def loadById(id: Int, user: User)(implicit ec: ExecutionContext): Future[PowerStation] =
    DataBase.PowerStation.getById(id, user) flatMap { _.withAssociatedVariation }

}