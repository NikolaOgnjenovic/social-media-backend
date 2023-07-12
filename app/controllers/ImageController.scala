package controllers

import dtos.NewImage
import models.Image
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.ImageService

@Singleton
class ImageController @Inject() (
    val controllerComponents: ControllerComponents,
    imageService: ImageService
)(implicit ec: ExecutionContext)
    extends BaseController {
  def create: Action[NewImage] =
    Action.async(parse.json[NewImage]) { request =>
      val imageToAdd: Image = request.body

      imageService.create(imageToAdd).map {
        case Some(image) => Created(Json.toJson(image))
        case None        => Conflict
      }
    }

  def getAll: Action[AnyContent] = Action.async {
    imageService.getAll.map(images => Ok(Json.toJson(images)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    imageService.getById(id).map {
      case Some(image) => Ok(Json.toJson(image))
      case None        => NotFound(s"Image with id: $id not found")
    }
  }

  def getByTag(tag: String): Action[AnyContent] = Action.async {
    imageService.getByTag(tag).map(images => Ok(Json.toJson(images)))
  }

  def getByTitle(title: String): Action[AnyContent] = Action.async {
    imageService.getByTitle(title).map(images => Ok(Json.toJson(images)))
  }

  def update(id: Long): Action[Image] = Action.async(parse.json[Image]) {
    request =>
      if (id != request.body.id)
        Future.successful(BadRequest("ID in path must be equal to id in body"))
      else
        imageService.update(id, request.body).map {
          case Some(image) => Ok(Json.toJson(image))
          case None        => NotFound
        }
  }
  def delete(id: Long): Action[AnyContent] = Action.async {
    imageService.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
