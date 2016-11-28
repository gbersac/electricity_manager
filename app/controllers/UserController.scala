package controllers

import javax.inject._

import model.DataBase
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class UserController(implicit exec: ExecutionContext) extends Controller {

  /**
    * Action to test if connection is valid.
    */
  def connect = Action.async(parse.json) {
  	???
  }

  def create = Action.async(parse.json) {
  	???
  }

}
