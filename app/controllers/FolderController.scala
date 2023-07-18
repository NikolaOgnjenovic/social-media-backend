package controllers

import dtos.NewFolder
import models.Folder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import repositories.FolderRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FolderController @Inject() (
    val controllerComponents: ControllerComponents,
    folderRepository: FolderRepository
)(implicit ec: ExecutionContext)
    extends BaseController {
  def create: Action[NewFolder] =
    Action.async(parse.json[NewFolder]) { request =>
      val folder: Folder = request.body

      folderRepository.create(folder).map {
        case Some(folder) => Created(Json.toJson(folder))
        case None         => Conflict
      }
    }

  def getAll: Action[AnyContent] = Action.async {
    folderRepository.getAll.map(images => Ok(Json.toJson(images)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    folderRepository.getById(id).map {
      case Some(folder) => Ok(Json.toJson(folder))
      case None         => NotFound(s"Folder with id: $id not found")
    }
  }

  def getByAuthorId(authorId: Long): Action[AnyContent] = Action.async {
    folderRepository.getByAuthorId(authorId).map {
      case Some(folder) => Ok(Json.toJson(folder))
      case None         => NotFound(s"Folder with author id: $authorId not found")
    }
  }

  def updateTitle(id: Long): Action[String] = Action.async(parse.json[String]) {
    request =>
      folderRepository.updateTitle(id, request.body).map {
        case Some(title) => Ok(Json.toJson(title))
        case None        => NotFound
      }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    folderRepository.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
