package model

import com.github.mauricio.async.db.RowData
import org.joda.time.{DateTime, LocalDateTime}
import play.api.libs.json.{JsValue, Json}
import utils.ControllerUtils.ElectricityManagerError

import scala.util.{Failure, Success, Try}

case class PowerVariation(
  execution: DateTime,
  delta: Int,
  station: PowerStation
) {

  def toJson: JsValue = Json.obj(
    "execution" -> execution.toString,
    "delta" -> delta
  )

}

object PowerVariation {
  val noPermissionError = "You don't have permission to use this power station"

  def fromDbResult(
    row: RowData,
    ps: PowerStation
  ): Either[String, PowerVariation] = {
    val t = if (row("station").asInstanceOf[Int] == ps.id) Try(PowerVariation(
      execution = row("execution_date").asInstanceOf[LocalDateTime].toDateTime,
      delta = row("delta").asInstanceOf[Int],
      station = ps
    ))
    else Try(throw ElectricityManagerError(noPermissionError))
    t match {
      case Success(pv) => Right(pv)
      case Failure(ElectricityManagerError(errorMsg)) => Left(errorMsg)
      case Failure(err) => Left(s"Malformed db row: ${err.getMessage}")
    }
  }

}
