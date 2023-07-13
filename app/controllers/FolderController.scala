package controllers

import dtos.NewFolder
import models.Folder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.FolderService

@Singleton
class FolderController @Inject() (
    val controllerComponents: ControllerComponents,
    folderService: FolderService
)(implicit ec: ExecutionContext)
    extends BaseController {
  def create: Action[NewFolder] =
    Action.async(parse.json[NewFolder]) { request =>
      val imageToAdd: Folder = request.body

      folderService.create(imageToAdd).map {
        case Some(folder) => Created(Json.toJson(folder))
        case None         => Conflict
      }
    }

  def getAll: Action[AnyContent] = Action.async {
    folderService.getAll.map(images => Ok(Json.toJson(images)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    folderService.getById(id).map {
      case Some(folder) => Ok(Json.toJson(folder))
      case None         => NotFound(s"Folder with id: $id not found")
    }
  }

  def getByAuthorId(authorId: Long): Action[AnyContent] = Action.async {
    folderService.getByAuthorId(authorId).map {
      case Some(folder) => Ok(Json.toJson(folder))
      case None         => NotFound(s"Folder with author id: $authorId not found")
    }
  }

  def updateTitle(id: Long): Action[String] = Action.async(parse.json[String]) {
    request =>
      folderService.updateTitle(id, request.body).map {
        case Some(title) => Ok(Json.toJson(title))
        case None        => NotFound
      }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    folderService.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
