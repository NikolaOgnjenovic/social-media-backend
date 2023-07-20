package controllers

import akka.stream.scaladsl.StreamConverters
import auth.JwtAction
import dtos.NewImage
import models.Image
import play.api.libs.Files
import play.api.libs.json.Json

import java.io.ByteArrayInputStream
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents,
  MultipartFormData
}
import repositories.CommentRepository
import repositories.ImageRepository

//  TODO: endpoint za register, generisi token na uspesan login, cuvam token na frontu nakon ulogovanja
//  u token body stavi user id
//  u svaki sledeci request dodajem token u header

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.MinioService

import java.nio.file.Paths

@Singleton
class ImageController @Inject() (
    val controllerComponents: ControllerComponents,
    imageRepository: ImageRepository,
    minioService: MinioService,
    commentRepository: CommentRepository,
    jwtAction: JwtAction
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
          val imagePath = s"public/images/$filename"
          picture.ref.copyTo(
            Paths.get(imagePath),
            replace = true
          )

          // Add the image to the database
          val imageToAdd: Image = new NewImage(
            request.body.dataParts.get("authorId").head.head.toLong,
            request.body.dataParts.get("tags").head.toList,
            request.body.dataParts.get("title").head.head,
            imagePath
          )

          imageRepository.create(imageToAdd).map {
            case Some(image) =>
              // Upload the image to minio and delete the temporary file
              minioService
                .upload("images", image.id.toString, imagePath)
              Paths.get(imagePath).toFile.delete()
              Created(Json.toJson(image))
            case None => Conflict
          }

        }
        .getOrElse(
          Redirect(routes.HomeController.index())
        )

      Ok("Image created")
    }
  }

  def getAll: Action[AnyContent] = jwtAction.async { request =>
    imageRepository
      .getAll(request.userId)
      .map(images => Ok(Json.toJson(images)))
  }

  // TODO: Add friends / editors so that not everyone can see all images
  def getById(id: Long): Action[AnyContent] = Action.async {
    imageRepository.getById(id).map {
      case Some(image) => Ok(Json.toJson(image))
      case None        => NotFound(s"Image with id: $id not found")
    }
  }

  def getImageFileById(id: Long): Action[AnyContent] = Action.async {
    minioService.get("images", id.toString).map {
      case Some(image) =>
        Ok.chunked(
          StreamConverters
            .fromInputStream(() => new ByteArrayInputStream(image))
        ).as("image/jpeg")
      case None => NotFound(s"Image with id: $id not found")
    }
  }

  def getByTags(tags: String): Action[AnyContent] = Action.async {
    val tagList = tags.split(",").toList
    imageRepository.getByTags(tagList).map(images => Ok(Json.toJson(images)))
  }

  def getByTitle(title: String): Action[AnyContent] = Action.async {
    imageRepository.getByTitle(title).map(images => Ok(Json.toJson(images)))
  }

  def getByFolderId(folderId: Long): Action[AnyContent] = Action.async {
    imageRepository
      .getByFolderId(folderId)
      .map(images => Ok(Json.toJson(images)))
  }

  def updateTags(id: Long): Action[List[String]] =
    jwtAction.async(parse.json[List[String]]) { request =>
      imageRepository.updateTags(request.userId, id, request.body).map {
        case Some(tags) => Ok(Json.toJson(tags))
        case None       => NotFound
      }
    }

  def updateLikeCount(id: Long): Action[Int] =
    jwtAction.async(parse.json[Int]) { request =>
      imageRepository.updateLikeCount(request.userId, id, request.body).map {
        case Some(likeCount) => Ok(Json.toJson(likeCount))
        case None            => NotFound
      }
    }

  def updateEditorIds(id: Long): Action[List[Long]] =
    jwtAction.async(parse.json[List[Long]]) { request =>
      imageRepository.updateEditorIds(request.userId, id, request.body).map {
        case Some(editorIds) => Ok(Json.toJson(editorIds))
        case None            => NotFound
      }
    }

  def updateFolderId(id: Long): Action[Long] =
    jwtAction.async(parse.json[Long]) { request =>
      imageRepository.updateFolderId(request.userId, id, request.body).map {
        case Some(folderId) => Ok(Json.toJson(folderId))
        case None           => NotFound
      }
    }

  def delete(id: Long): Action[AnyContent] = jwtAction.async { request =>
    imageRepository.delete(request.userId, id).map {
      case Some(_) =>
        minioService.remove("images", id.toString).map {
          case Some(_) =>
            commentRepository.deleteByImageId(id).map {
              case Some(_) => NoContent
              case None    => NotFound
            }
          case None => NotFound
        }
        NoContent
      case None => NotFound
    }
  }
}
