package controllers

import akka.pattern.StatusReply.Success
import auth.JwtAction
import dtos.NewUser
import models.User
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import repositories.UserRepository
import services.JwtService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    userRepository: UserRepository,
    jwtService: JwtService
)(
    implicit ec: ExecutionContext,
    implicit val conf: Configuration,
    jwtAction: JwtAction
) extends BaseController {
  def create: Action[NewUser] = {
    userRepository.createTable()
    Action.async(parse.json[NewUser]) { request =>
      val user: User = request.body
      userRepository.create(user).flatMap {
        case Some(u) =>
          jwtService.generateToken(u.id).map { token =>
            val jsonResponse = Json.obj(
              "userId" -> u.id,
              "token" -> token,
              "likedImageIds" -> u.likedImageIds,
              "likedCommentIds" -> u.likedCommentIds
            )
            Created(jsonResponse)
          }

        case None => Future.successful(Conflict)
      }
    }
  }

  def login: Action[NewUser] = {
    Action.async(parse.json[NewUser]) { request =>
      val user: User = request.body
      userRepository.login(user).flatMap {
        case Some(u) =>
          jwtService.generateToken(u.id).map { token =>
            val jsonResponse = Json.obj(
              "userId" -> u.id,
              "token" -> token,
              "likedImageIds" -> u.likedImageIds,
              "likedCommentIds" -> u.likedCommentIds
            )
            Ok(jsonResponse)
          }

        case None => Future.successful(Conflict)
      }
    }
  }

  def logout(): Action[String] = {
    Action.async(parse.json[String]) { request =>
      jwtService.blacklistToken(request.body).map {
        case Some(_) => Ok
        case None    => Conflict
      }
    }
  }
  def getAll: Action[AnyContent] = Action.async {
    userRepository.getAll.map(users => Ok(Json.toJson(users)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    userRepository.getById(id).map {
      case Some(user) => Ok(Json.toJson(user))
      case None       => NotFound(s"User with id: $id not found")
    }
  }

  def updateLikedImageIds(): Action[List[Long]] =
    jwtAction.async(parse.json[List[Long]]) { request =>
      userRepository
        .updateLikedImageIds(request.userId, request.body)
        .map(likedImageIds => Ok(Json.toJson(likedImageIds)))
    }

  def updateLikedCommentIds(): Action[List[Long]] =
    jwtAction.async(parse.json[List[Long]]) { request =>
      userRepository
        .updateLikedCommentIds(request.userId, request.body)
        .map(likedCommentIds => Ok(Json.toJson(likedCommentIds)))
    }

  def updatePassword(): Action[String] =
    jwtAction.async(parse.json[String]) { request =>
      userRepository.updatePassword(request.userId, request.body).map {
        case Some(password) => Ok(Json.toJson(password))
        case None           => NotFound(s"User with id: ${request.userId} not found")
      }
    }

  def delete(id: Long): Action[AnyContent] = Action.async {
    userRepository.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound(s"User with id: $id not found")
    }
  }
}
