package controllers

import akka.stream.scaladsl.StreamConverters
import auth.JwtAction
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import dtos.NewImage
import play.api.libs.Files
import play.api.libs.json.Json

import java.io.{ByteArrayInputStream, File}
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents,
  MultipartFormData
}
import repositories.CommentRepository
import repositories.ImageRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.MinioService
import models.Image

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
            request.body.dataParts.get("title").head.head
          )

          imageRepository.create(imageToAdd).map {
            case Some(image) =>
              // Upload the image to minio and delete the temporary file
              minioService
                .upload("images", image.id.toString, imagePath)

              // Upload the compressed image to minio and delete the temporary file
              val compressedImagePath = compressImageFile(
                Paths.get(imagePath).toFile,
                s"public/images/compressed/$filename"
              )
              minioService.upload(
                "compressed-images",
                image.id.toString,
                compressedImagePath
              )

              Paths.get(imagePath).toFile.delete()
              Paths.get(compressedImagePath).toFile.delete()

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

  private def compressImageFile(
      imageFile: File,
      compressedPath: String
  ): String = {
    val image = ImmutableImage.loader().fromFile(imageFile)
    val writer = new JpegWriter().withCompression(10).withProgressive(true)
    image.output(writer, new File(compressedPath))
    compressedPath
  }

  def getAll: Action[AnyContent] = Action.async {
    imageRepository.getAll.map(images => Ok(Json.toJson(images)))
  }
  def getAllByUserId: Action[AnyContent] = jwtAction.async { request =>
    imageRepository
      .getAllByUserId(request.userId)
      .map(images => Ok(Json.toJson(images)))
  }

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

  def getCompressedImageFileById(id: Long): Action[AnyContent] = Action.async {
    minioService.get("compressed-images", id.toString).map {
      case Some(image) =>
        Ok.chunked(
          StreamConverters
            .fromInputStream(() => new ByteArrayInputStream(image))
        ).as("image/jpeg")
      case None => NotFound(s"Compressed image with id: $id not found")
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
      imageRepository.updateLikeCount(id, request.body).map {
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
        minioService.remove("compressed-images", id.toString).map {
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
