package controllers

import javax.inject._

import model.DataBase
import play.api.libs.json.JsResultException
import play.api.mvc._
import utils.Utils

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
    Utils.executeWithLoggedUser(request) { _ =>
      Future.successful(Ok(Utils.successBody))
    }
  }

  def create = Action.async(parse.json) { request =>
    val t = Try ((
      (request.body \ "pseudo").as[String],
      (request.body \ "password").as[String]
    )) map { case (pseudo: String, password: String) =>
      if (pseudo.length < 4 || password.length < 4)
        Utils.failureResponse(badUserCreationParamsError, BAD_REQUEST)
      else DataBase.User.alreadyExist(pseudo) flatMap { alreadyExist =>
        if (alreadyExist)
          Utils.failureResponse(s"pseudo $pseudo already used", BAD_REQUEST)
        else DataBase.User.createUser(pseudo, password) map { queryResult =>
          if (queryResult.rowsAffected == 1)
            Ok(Utils.successBody)
          else
            BadRequest(Utils.failureBody(queryResult.statusMessage))
        }
      }
    } recover {
      case JsResultException(_) => Utils.failureResponse(Utils.missingUserInfoError, BAD_REQUEST)
      case NonFatal(err) => Utils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
    }
    t.getOrElse(Utils.failureResponse(Utils.unexpectedError, INTERNAL_SERVER_ERROR))
  }

}
