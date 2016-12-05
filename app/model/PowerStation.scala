package model

import com.github.mauricio.async.db.RowData
import play.api.libs.json._
import utils.ControllerUtils.ElectricityManagerError

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

}
