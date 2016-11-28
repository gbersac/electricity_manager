package controllers

import javax.inject._

import model.DataBase
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class PowerStationController(implicit exec: ExecutionContext) extends Controller {

 def create = Action.async(parse.json) {
 	???
 }

 def use = Action.async(parse.json) {
 	???
 }

 def usage = Action.async(parse.json) {
 	???
 }

}
