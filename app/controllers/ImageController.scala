package controllers

import dtos.NewImage
import models.Image
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents,
  MultipartFormData
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.ImageService

import java.nio.file.Paths

@Singleton
class ImageController @Inject() (
    val controllerComponents: ControllerComponents,
    imageService: ImageService
)(implicit ec: ExecutionContext)
    extends BaseController {
  def create: Action[MultipartFormData[Files.TemporaryFile]] = {
    Action(parse.multipartFormData) { request =>
      request.body.dataParts("authorId")
      request.body
        .file("image")
        .map { picture =>
          // Upload the picture
          val filename = Paths.get(picture.filename).getFileName
          picture.ref.copyTo(
            Paths.get(s"public/images/$filename"),
            replace = true
          )

          // Add the image to the database
          val imageToAdd: Image = new NewImage(
            request.body.dataParts.get("authorId").head.head.toLong,
            request.body.dataParts.get("tags").head.toList,
            request.body.dataParts.get("title").head.head,
            s"public/images/$filename"
          )
          imageService.create(imageToAdd).map {
            case Some(image) => Created(Json.toJson(image))
            case None        => Conflict
          }
        }
        .getOrElse(
          Redirect(routes.HomeController.index())
        )
      Ok("Image created")
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

  def getByTags(tags: String): Action[AnyContent] = Action.async {
    val tagList = tags.split(",").toList
    imageService.getByTags(tagList).map(images => Ok(Json.toJson(images)))
  }

  def getByTitle(title: String): Action[AnyContent] = Action.async {
    imageService.getByTitle(title).map(images => Ok(Json.toJson(images)))
  }

  def getByFolderId(folderId: Long): Action[AnyContent] = Action.async {
    imageService.getByFolderId(folderId).map(images => Ok(Json.toJson(images)))
  }

  def updateTags(id: Long): Action[List[String]] =
    Action.async(parse.json[List[String]]) { request =>
      imageService.updateTags(id, request.body).map {
        case Some(tags) => Ok(Json.toJson(tags))
        case None       => NotFound
      }
    }

  def updatelikeCount(id: Long): Action[Int] =
    Action.async(parse.json[Int]) { request =>
      imageService.updatelikeCount(id, request.body).map {
        case Some(likeCount) => Ok(Json.toJson(likeCount))
        case None        => NotFound
      }
    }

  def updateEditorIds(id: Long): Action[List[Long]] =
    Action.async(parse.json[List[Long]]) { request =>
      imageService.updateEditorIds(id, request.body).map {
        case Some(editorIds) => Ok(Json.toJson(editorIds))
        case None            => NotFound
      }
    }

  def updateFolderId(id: Long): Action[Long] =
    Action.async(parse.json[Long]) { request =>
      imageService.updateFolderId(id, request.body).map {
        case Some(folderId) => Ok(Json.toJson(folderId))
        case None           => NotFound
      }
    }

  def delete(id: Long): Action[AnyContent] = Action.async {
    imageService.delete(id).map {
      case Some(_) => NoContent
      case None    => NotFound
    }
  }
}
