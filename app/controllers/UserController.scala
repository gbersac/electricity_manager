package controllers

import javax.inject._

import model.DataBase
import play.api.libs.json.JsResultException
import play.api.mvc._
import utils.Utils

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.control.NonFatal
import DataBase.DbRequestError

@Singleton
class UserController @Inject() (implicit exec: ExecutionContext) extends Controller {
  val missingInfoError = "Body should contain a json object with keys pseudo and password"
  val unexpectedError = "Unexpected error"
  val badUserCreationParamsError = "Pseudo and password should have more than 4 characters"
  val invalidPassword = "User password is not correct"

  /**
    * Action to test if connection is valid.
    */
  def connect = Action.async(parse.json) { request =>
    val t = Try((
      (request.body \ "pseudo").as[String],
      (request.body \ "password").as[String]
    )) map { case (pseudo: String, password: String) =>
      DataBase.User.getUserByPseudo(pseudo) map { user =>
        if (user.isPasswordCorrect(password))
          Ok(Utils.successBody)
        else
          BadRequest(Utils.failureBody(invalidPassword))
      } recover { case DbRequestError(errorMsg) => BadRequest(Utils.failureBody(errorMsg))}
    } recover {
      case JsResultException(_) => Utils.failureResponse(missingInfoError, BAD_REQUEST)
      case DataBase.DbRequestError(errorMsg) => Utils.failureResponse(errorMsg, BAD_REQUEST)
      case NonFatal(err) => Utils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
    }
    t.getOrElse(Utils.failureResponse(unexpectedError, INTERNAL_SERVER_ERROR))
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
          if (DataBase.queryResultIsSuccess(queryResult))
            Ok(Utils.successBody)
          else
            BadRequest(Utils.failureBody(queryResult.statusMessage))
        }
      }
    } recover {
      case JsResultException(_) => Utils.failureResponse(missingInfoError, BAD_REQUEST)
      case NonFatal(err) => Utils.failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
    }
    t.getOrElse(Utils.failureResponse(unexpectedError, INTERNAL_SERVER_ERROR))
  }

}
