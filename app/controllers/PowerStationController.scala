package controllers

import javax.inject._

import model.DataBase
import play.api.mvc._

import scala.concurrent.ExecutionContext
import model.DataBase
import play.api.libs.json.JsResultException
import play.api.mvc._
import utils.Utils

import scala.concurrent.{ExecutionContext, Future}
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

 def use = Action.async(parse.json) {
 	???
 }

 def usage = Action.async(parse.json) {
 	???
 }

}

object PowerStationController {
  val incorrectCapacityError = "Capacity should be positive."
  val missingPowerStationInfosError = "Body should contain typePW, code and maxCapacity."
}