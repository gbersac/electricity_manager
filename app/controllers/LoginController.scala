package controllers

import model.{DBQueries, User}
import play.api.libs.json.{JsResultException, JsValue}
import play.api.mvc.{Controller, Request, Result}
import utils.{ControllerUtils, DBConnectionPool}
import utils.ControllerUtils.{ElectricityManagerError, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

object LoginController extends Controller {
  val invalidPassword = "User password is not correct"
  val missingUserInfoError = "Body should contain a json object with keys pseudo and password"

  def executeWithLoggedUser(
    input: Request[JsValue]
  )(
    f: User => Future[Result]
  )(
    implicit ec: ExecutionContext,
    db: DBConnectionPool
  ): Future[Result] = {
    val t = Try((
      (input.body \ "pseudo").as[String],
      (input.body \ "password").as[String]
      )) map { case (pseudo: String, password: String) =>
      DBQueries.User.getUserByPseudo(pseudo) flatMap { user =>
        if (user.isPasswordCorrect(password))
          f(user)
        else
          failureResponse(LoginController.invalidPassword, BAD_REQUEST)
      } recover { case ElectricityManagerError(errorMsg) => BadRequest(failureBody(errorMsg))}
    } recover {
      case JsResultException(_) => failureResponse(LoginController.missingUserInfoError, BAD_REQUEST)
      case NonFatal(err) => failureResponse(err.getMessage, INTERNAL_SERVER_ERROR)
    }
    t.getOrElse(failureResponse(ControllerUtils.unexpectedError, INTERNAL_SERVER_ERROR))
  }

}
