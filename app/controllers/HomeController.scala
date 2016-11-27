package controllers

import javax.inject._

import model.DataBase
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject() extends Controller {

  def index = Action.async {
    DataBase.getCities map { results => Ok(results)}
  }

}
