package controllers

import auth.JwtAction
import dtos.NewUser
import models.User
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import repositories.UserRepository
import services.JwtService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    userRepository: UserRepository,
    jwtService: JwtService
)(
    implicit ec: ExecutionContext,
    implicit val conf: Configuration
) extends BaseController {
  def create: Action[NewUser] = {
    Action.async(parse.json[NewUser]) { request =>
      val user: User = request.body
      userRepository.create(user).map {
        case Some(u) =>
          val token = jwtService.generateToken(u.id)
          val jsonResponse = Json.obj(
            "userId" -> u.id,
            "token" -> token
          )
          Created(jsonResponse)
        case None => Conflict
      }
    }
  }

  def login: Action[NewUser] = {
    Action.async(parse.json[NewUser]) { request =>
      val user: User = request.body
      userRepository.login(user).map {
        case Some(u) =>
          val token = jwtService.generateToken(u.id)
          val jsonResponse = Json.obj(
            "userId" -> u.id,
            "token" -> token
          )
          Ok(jsonResponse)
        case None => Conflict
      }
    }
  }

  // TODO: Add user roles to user profiles. If admin parameter = true in token, then the user can getAll etc.
  def getAll: Action[AnyContent] = Action.async {
    userRepository.getAll.map(users => Ok(Json.toJson(users)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    userRepository.getById(id).map {
      case Some(user) => Ok(Json.toJson(user))
      case None       => NotFound(s"User with id: $id not found")
    }
  }

  def updatePassword(id: Long): Action[String] =
    Action.async(parse.json[String]) { request =>
      userRepository.updatePassword(id, request.body).map {
        case Some(password) => Ok(Json.toJson(password))
        case None           => NotFound(s"User with id: $id not found")
      }
    }

  def delete(id: Long): Action[AnyContent] = Action.async {
    userRepository.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound(s"User with id: $id not found")
    }
  }
}
