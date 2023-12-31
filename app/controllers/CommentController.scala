package controllers

import auth.JwtAction
import dtos.NewComment
import models.Comment
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import repositories.CommentRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CommentController @Inject() (
    val controllerComponents: ControllerComponents,
    commentRepository: CommentRepository,
    jwtAction: JwtAction
)(implicit ec: ExecutionContext)
    extends BaseController {

  // Returns a comment with the newly generated id
  def create: Action[NewComment] =
    Action.async(parse.json[NewComment]) { request =>
      val comment: Comment = request.body

      commentRepository.create(comment).map {
        case Some(comment) => Created(Json.toJson(comment))
        case None          => Conflict
      }
    }

  def getAll: Action[AnyContent] = Action.async {
    commentRepository.getAll.map(comments => Ok(Json.toJson(comments)))
  }
  def getAllByUserId: Action[AnyContent] = jwtAction.async { request =>
    commentRepository
      .getAllByUserId(request.userId)
      .map(comments => Ok(Json.toJson(comments)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    commentRepository.getById(id).map {
      case Some(comment) => Ok(Json.toJson(comment))
      case None          => NotFound(s"Comment with id: $id not found")
    }
  }

  def getByAuthorId(authorId: Long): Action[AnyContent] = Action.async {
    commentRepository.getByAuthorId(authorId).map {
      case Some(comment) => Ok(Json.toJson(comment))
      case None          => NotFound(s"Comment with author id: $authorId not found")
    }
  }

  def getByImageId(imageId: Long): Action[AnyContent] = Action.async {
    commentRepository.getByImageId(imageId).map {
      case Some(comment) => Ok(Json.toJson(comment))
      case None          => NotFound(s"Comment with image id: $imageId not found")
    }
  }

  def updateContent(id: Long): Action[String] =
    jwtAction.async(parse.json[String]) { request =>
      commentRepository.updateContent(request.userId, id, request.body).map {
        case Some(content) => Ok(Json.toJson(content))
        case None          => NotFound
      }
    }

  def updateLikeCount(id: Long): Action[Int] =
    Action.async(parse.json[Int]) { request =>
      commentRepository.updateLikeCount(id, request.body).map {
        case Some(likeCount) => Ok(Json.toJson(likeCount))
        case None            => NotFound
      }
    }

  def delete(id: Long): Action[AnyContent] = jwtAction.async { request =>
    commentRepository.delete(request.userId, id).map {
      case Some(commentId) => Accepted(Json.toJson(commentId))
      case None            => NotFound
    }
  }

  def deleteByImageId(imageId: Long): Action[AnyContent] = Action.async {
    commentRepository.deleteByImageId(imageId).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
