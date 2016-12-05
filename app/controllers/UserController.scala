package controllers

import javax.inject._

import model.DBQueries
import play.api.libs.json.JsResultException
import play.api.mvc._
import utils.ControllerUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
class UserController @Inject() (implicit exec: ExecutionContext) extends Controller {
  val badUserCreationParamsError = "Pseudo and password should have more than 4 characters"

  /**
    * Action to test if connection is valid.
    */
  def connect = Action.async(parse.json) { request =>
    LoginController.executeWithLoggedUser(request) { _ =>
      Future.successful(Ok(ControllerUtils.successBody))
    }
  }

  def create = Action.async(parse.json) { request =>
    val t = Try ((
      (request.body \ "pseudo").as[String],
      (request.body \ "password").as[String]
    )) map { case (pseudo: String, password: String) =>
      if (pseudo.length < 4 || password.length < 4)
        ControllerUtils.failureResponse(badUserCreationParamsError, BAD_REQUEST)
      else DBQueries.User.alreadyExist(pseudo) flatMap { alreadyExist =>
        if (alreadyExist)
          ControllerUtils.failureResponse(s"pseudo $pseudo already used", BAD_REQUEST)
        else DBQueries.User.createUser(pseudo, password) map { queryResult =>
          if (queryResult.rowsAffected == 1)
            Ok(ControllerUtils.successBody)
          else
            BadRequest(ControllerUtils.failureBody(queryResult.statusMessage))
        }
      }
    } recover {
      case JsResultException(_) => ControllerUtils.failureResponse(LoginController.missingUserInfoError, BAD_REQUEST)
      case NonFatal(err) => ControllerUtils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
    }
    t.getOrElse(ControllerUtils.failureResponse(ControllerUtils.unexpectedError, INTERNAL_SERVER_ERROR))
  }

}
