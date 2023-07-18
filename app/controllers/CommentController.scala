package controllers

import dtos.NewComment
import models.Comment
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import repositories.CommentRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}

@Singleton
class CommentController @Inject() (
    val controllerComponents: ControllerComponents,
    commentRepository: CommentRepository
)(implicit ec: ExecutionContext)
    extends BaseController {
  def create: Action[NewComment] =
    Action.async(parse.json[NewComment]) { request =>
      val comment: Comment = request.body

      commentRepository.create(comment).map {
        case Some(comment) => Created(Json.toJson(comment))
        case None          => Conflict
      }
    }

  def getAll: Action[AnyContent] = Action.async {
    commentRepository.getAll.map(images => Ok(Json.toJson(images)))
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
    Action.async(parse.json[String]) { request =>
      commentRepository.updateContent(id, request.body).map {
        case Some(content) => Ok(Json.toJson(content))
        case None          => NotFound
      }
    }

  def updateLikeCount(id: Long): Action[Int] = Action.async(parse.json[Int]) {
    request =>
      commentRepository.updateLikeCount(id, request.body).map {
        case Some(likeCount) => Ok(Json.toJson(likeCount))
        case None            => NotFound
      }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    commentRepository.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }

  def deleteByImageId(imageId: Long): Action[AnyContent] = Action.async {
    commentRepository.deleteByImageId(imageId).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
