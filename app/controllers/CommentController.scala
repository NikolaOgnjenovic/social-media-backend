package controllers

import dtos.NewComment
import models.Comment
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.CommentService

@Singleton
class CommentController @Inject() (
    val controllerComponents: ControllerComponents,
    commentService: CommentService
)(implicit ec: ExecutionContext)
    extends BaseController {
  def create: Action[NewComment] =
    Action.async(parse.json[NewComment]) { request =>
      val imageToAdd: Comment = request.body

      commentService.create(imageToAdd).map {
        case Some(comment) => Created(Json.toJson(comment))
        case None          => Conflict
      }
    }

  def getAll: Action[AnyContent] = Action.async {
    commentService.getAll.map(images => Ok(Json.toJson(images)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    commentService.getById(id).map {
      case Some(comment) => Ok(Json.toJson(comment))
      case None          => NotFound(s"Comment with id: $id not found")
    }
  }

  def getByAuthorId(authorId: Long): Action[AnyContent] = Action.async {
    commentService.getByAuthorId(authorId).map {
      case Some(comment) => Ok(Json.toJson(comment))
      case None          => NotFound(s"Comment with author id: $authorId not found")
    }
  }

  def updateContent(id: Long): Action[String] =
    Action.async(parse.json[String]) { request =>
      commentService.updateContent(id, request.body).map {
        case Some(content) => Ok(Json.toJson(content))
        case None          => NotFound
      }
    }

  def updatelikeCount(id: Long): Action[Int] = Action.async(parse.json[Int]) {
    request =>
      commentService.updatelikeCount(id, request.body).map {
        case Some(likeCount) => Ok(Json.toJson(likeCount))
        case None        => NotFound
      }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    commentService.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
