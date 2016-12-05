package controllers

import javax.inject._

import model.DBQueries
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import utils.ControllerUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class PowerStationController @Inject() (implicit exec: ExecutionContext) extends Controller {

  def create = Action.async(parse.json) { request =>
    LoginController.executeWithLoggedUser(request) { user =>
      val t = Try(
        (request.body \ "typePW").as[String],
        (request.body \ "code").as[String],
        (request.body \ "maxCapacity").as[Int]
      ) map { case (typePW, code, maxCapacity) =>
        if (maxCapacity <= 0)
          ControllerUtils.failureResponse(PowerStationController.incorrectCapacityError, BAD_REQUEST)
        else DBQueries.PowerStation.create(typePW, code, maxCapacity, user) map { queryResult =>
          if (queryResult.rowsAffected == 1)
            Ok(ControllerUtils.successBody)
          else
            BadRequest(ControllerUtils.failureBody(queryResult.statusMessage))
        }
      } recover {
        case JsResultException(_) =>
          ControllerUtils.failureResponse(PowerStationController.missingPowerStationInfosError, BAD_REQUEST)
        case NonFatal(err) => ControllerUtils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
      }
      t.getOrElse(ControllerUtils.failureResponse(ControllerUtils.unexpectedError, INTERNAL_SERVER_ERROR))
    }
  }

  def use = Action.async(parse.json) { request =>
    LoginController.executeWithLoggedUser(request) { user =>
      val t = Try(
        (request.body \ "delta").as[Int],
        (request.body \ "stationId").as[Int]
      ) map { case (delta: Int, stationId) =>
        DBQueries.PowerStation.getById(stationId, user) flatMap { powerStation =>
          val newEnergyLevel = powerStation.currentEnergy + delta
          if (newEnergyLevel < 0 || newEnergyLevel > powerStation.maxCapacity)
            ControllerUtils.failureResponse(
              s"Incorrect delta, new energy level can't be over ${powerStation.maxCapacity} or inferior to 0.",
              BAD_REQUEST
            )
          else DBQueries.PowerVariation.create(powerStation, delta) map { queryResult =>
            if (queryResult.rowsAffected == 1) Ok(Json.obj(
              "status" -> "success",
              "newEnergyLevel" -> newEnergyLevel
            )) else BadRequest(ControllerUtils.failureBody(
              s"Energy variation insert failed, request status : ${queryResult.statusMessage}"
            ))
          }
        }
      } recover {
        case JsResultException(_) =>
          ControllerUtils.failureResponse(PowerStationController.missingPowerVariationInfosError, BAD_REQUEST)
        case NonFatal(err) => ControllerUtils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
      }
      t.getOrElse(ControllerUtils.failureResponse(ControllerUtils.unexpectedError, INTERNAL_SERVER_ERROR))
    }
  }

  def powerVariationHistory = Action.async(parse.json) { request =>
    LoginController.executeWithLoggedUser(request) { user =>
      DBQueries.PowerStation.allOwnedByUser(user) flatMap { Future.sequence(_) } map { powerStations =>
        val json = Json.toJson(powerStations.map(_.toJson))
        Ok(json)
      } recover {
        case NonFatal(err) => InternalServerError(ControllerUtils.failureBody(err.getMessage))
      }
    }
  }

}

object PowerStationController {
  val incorrectCapacityError = "Capacity should be positive."
  val missingPowerStationInfosError = "Body should contain typePW, code and maxCapacity."
  val missingPowerVariationInfosError = "Body should contain delta and stationId."
}