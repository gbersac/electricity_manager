package controllers

import javax.inject._

import model.DataBase
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
class UserController @Inject() (implicit exec: ExecutionContext) extends Controller {

  /**
    * Action to test if connection is valid.
    */
  def connect = Action.async(parse.json) { request =>
    ???
  }

  def create = Action.async(parse.json) { request =>
    val t = Try ((
      (request.body \ "pseudo").as[String],
      (request.body \ "password").as[String]
    )) map { case (pseudo, password) =>
      if (pseudo.size < 4 || password.size < 4)
        Future.successful(BadRequest(Json.obj(
          "status" -> "failure",
          "reason" -> s"pseudo and password should be under 4 characters"
        )))
      else DataBase.User.alreadyExist(pseudo) flatMap { alreadyExist =>
        if (alreadyExist)
          Future.successful(BadRequest(Json.obj(
            "status" -> "failure",
            "reason" -> s"pseudo $pseudo already used"
          )))
        else DataBase.User.createUser(pseudo, password) map { queryResult =>
          if (DataBase.queryResultIsSuccess(queryResult))
            Ok(Json.obj("status" -> "success"))
          else
            BadRequest(Json.obj("status" -> queryResult.statusMessage))
        }
      }
    } recover {
      case JsResultException(_) => Future.successful(BadRequest(Json.obj(
        "status" -> "failure",
        "reason" -> "body should contain a json object with keys pseudo and password"
      )))
      case NonFatal(err) => Future.successful(InternalServerError(Json.obj(
          "status" -> "failure",
          "reason" -> err.getMessage
        )))
    }
    t.getOrElse(Future.successful(InternalServerError(Json.obj("status" -> "failure"))))
  }

}
