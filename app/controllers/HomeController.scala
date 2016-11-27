package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import utils.DataBase
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject() extends Controller {

  def index = Action.async {
    DataBase.getCities map { results => Ok(results mkString "\n")}
  }

}
