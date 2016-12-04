package controllers

import javax.inject._

import model.{DataBase, PowerStation}
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import utils.Utils

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.control.NonFatal

class PowerStationController @Inject() (implicit exec: ExecutionContext) extends Controller {

  def create = Action.async(parse.json) { request =>
    Utils.executeWithLoggedUser(request) { user =>
      val t = Try(
        (request.body \ "typePW").as[String],
        (request.body \ "code").as[String],
        (request.body \ "maxCapacity").as[Int]
      ) map { case (typePW, code, maxCapacity) =>
        if (maxCapacity <= 0)
          Utils.failureResponse(PowerStationController.incorrectCapacityError, BAD_REQUEST)
        else DataBase.PowerStation.create(typePW, code, maxCapacity, user) map { queryResult =>
          if (queryResult.rowsAffected == 1)
            Ok(Utils.successBody)
          else
            BadRequest(Utils.failureBody(queryResult.statusMessage))
        }
      } recover {
        case JsResultException(_) =>
          Utils.failureResponse(PowerStationController.missingPowerStationInfosError, BAD_REQUEST)
        case NonFatal(err) => Utils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
      }
      t.getOrElse(Utils.failureResponse(Utils.unexpectedError, INTERNAL_SERVER_ERROR))
    }
  }

  def use = Action.async(parse.json) { request =>
    Utils.executeWithLoggedUser(request) { user =>
      val t = Try(
        (request.body \ "delta").as[Int],
        (request.body \ "stationId").as[Int]
      ) map { case (delta: Int, stationId) =>
        PowerStation.loadById(stationId, user) flatMap { powerStation =>
          val newEnergyLevel = powerStation.currentEnergy + delta
          if (newEnergyLevel < 0 || newEnergyLevel > powerStation.maxCapacity)
            Utils.failureResponse(
              s"Incorrect delta, new energy level can't be over ${powerStation.maxCapacity} or inferior to 0.",
              BAD_REQUEST
            )
          else DataBase.PowerVariation.create(powerStation, delta) map { queryResult =>
            if (queryResult.rowsAffected == 1) Ok(Json.obj(
              "status" -> "success",
              "newEnergyLevel" -> newEnergyLevel
            )) else BadRequest(Utils.failureBody(
              s"Energy variation insert failed, request status : ${queryResult.statusMessage}"
            ))
          }
        }
      } recover {
        case JsResultException(_) =>
          Utils.failureResponse(PowerStationController.missingPowerStationInfosError, BAD_REQUEST)
        case NonFatal(err) => Utils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
      }
      t.getOrElse(Utils.failureResponse(Utils.unexpectedError, INTERNAL_SERVER_ERROR))
    }
  }

  def powerVariationHistory = Action.async(parse.json) { request =>
    Utils.executeWithLoggedUser(request) { user =>
      ???
    }
  }

}

object PowerStationController {
  val incorrectCapacityError = "Capacity should be positive."
  val missingPowerStationInfosError = "Body should contain typePW, code and maxCapacity."
}