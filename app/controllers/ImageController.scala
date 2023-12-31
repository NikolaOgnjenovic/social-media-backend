package controllers

import akka.stream.scaladsl.StreamConverters
import auth.JwtAction
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import dtos.NewImage
import play.api.libs.Files
import play.api.libs.json.Json

import scala.util.{Failure, Success}
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
import scala.concurrent.{ExecutionContext, Future}
import services.MinioService
import models.Image
import org.slf4j.LoggerFactory
import play.api.Configuration

import java.nio.file.Paths
@Singleton
class ImageController @Inject() (
    val controllerComponents: ControllerComponents,
    imageRepository: ImageRepository,
    minioService: MinioService,
    commentRepository: CommentRepository,
    jwtAction: JwtAction
)(implicit ec: ExecutionContext, config: Configuration)
    extends BaseController {
  private val logger = LoggerFactory.getLogger(getClass)

  def create: Action[MultipartFormData[Files.TemporaryFile]] = {
    Action(parse.multipartFormData).async { request =>
      request.body
        .file("image")
        .map { picture =>
          // Upload the picture
          val filename = Paths.get(picture.filename).getFileName
          val uploadDir = config.get[String]("imageUploadDirectory")
          val imagePath = s"$uploadDir/$filename"
          picture.ref.copyTo(
            Paths.get(imagePath),
            replace = true
          )

          // Add the image to the database
          val imageToAdd: Image = new NewImage(
            request.body.dataParts.get("authorId").head.head.toLong,
            request.body.dataParts
              .get("tags")
              .flatMap(_.headOption)
              .getOrElse("")
              .stripPrefix("[")
              .stripSuffix("]")
              .split(",")
              .map(_.trim.stripPrefix("\"").stripSuffix("\""))
              .toList,
            request.body.dataParts.get("title").head.head
          )

          imageRepository.create(imageToAdd).flatMap {
            case Some(createdImage) =>
              logger.info("Image path on first create: " + imagePath)
              logger.info(
                "minio endpoint: " + config.get[String]("minio.endpoint")
              )
              // Upload the image to minio and delete the temporary file
              minioService
                .upload("images", createdImage.id.toString, imagePath) match {
                case Failure(ex) =>
                  logger.error("Failed first upload with: " + ex.getMessage)
                  Future.successful(Conflict(ex.getMessage))
                case Success(_) =>
                  logger.info("Uploading compressed image...")
                  // Upload the compressed image to minio and delete the temporary file
                  val compressedUploadDir =
                    config.get[String]("compressedImageUploadDirectory")
                  val compressedFilename = Paths.get(imagePath).getFileName
                  logger
                    .info("Compressed upload directory: " + compressedUploadDir)
                  logger.info("Compressed filename: " + compressedFilename)
                  val compressedImagePath = compressImageFile(
                    Paths.get(imagePath).toFile,
                    s"$compressedUploadDir/$compressedFilename"
                  )
                  logger.info("Compressed image path: " + compressedImagePath)
                  logger.info(
                    "minio endpoint: " + config.get[String]("minio.endpoint")
                  )
                  minioService.upload(
                    "compressed-images",
                    createdImage.id.toString,
                    compressedImagePath
                  ) match {
                    case Failure(ex) =>
                      Future.successful(Conflict(ex.getMessage))
                    case Success(_) =>
                      Paths.get(imagePath).toFile.delete()
                      Paths.get(compressedImagePath).toFile.delete()

                      Future.successful(Created(Json.toJson(createdImage)))
                  }
              }

            case None => Future.successful(Conflict)
          }
        }
        .getOrElse {
          Future.successful(BadRequest("Image file not found"))
        }
    }
  }

  def getAll: Action[AnyContent] = Action.async {
    imageRepository.getAll.map(images => Ok(Json.toJson(images)))
  }

  def getAllWithPagination(page: Int, pageSize: Int): Action[AnyContent] =
    Action.async {
      if (page <= 0) {
        Future.successful(
          BadRequest(
            "Invalid page number. The page number must be a positive number"
          )
        )
      } else {
        val offset = (page - 1) * pageSize
        imageRepository
          .getAllWithPagination(offset, pageSize)
          .map(images => Ok(Json.toJson(images)))
      }
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

  def getImageFileById(id: Long): Action[AnyContent] = Action {
    minioService.get("images", id.toString) match {
      case Success(image) =>
        Ok.chunked(
          StreamConverters
            .fromInputStream(() => new ByteArrayInputStream(image))
        ).as("image/jpeg")
      case Failure(_) => NotFound(s"Image with id: $id not found")
    }
  }

  def getCompressedImageFileById(id: Long): Action[AnyContent] = Action {
    minioService.get("compressed-images", id.toString) match {
      case Success(image) =>
        Ok.chunked(
          StreamConverters
            .fromInputStream(() => new ByteArrayInputStream(image))
        ).as("image/jpeg")
      case Failure(_) => NotFound(s"Compressed image with id: $id not found")
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

  def updateFile(id: Long): Action[MultipartFormData[Files.TemporaryFile]] =
    jwtAction(parse.multipartFormData) { request =>
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

          // Upload the image to minio and delete the temporary file
          minioService
            .upload("images", id.toString, imagePath)

          // Upload the compressed image to minio and delete the temporary file
          val compressedImagePath = compressImageFile(
            Paths.get(imagePath).toFile,
            s"public/images/compressed/$filename"
          )
          minioService.upload(
            "compressed-images",
            id.toString,
            compressedImagePath
          )

          Paths.get(imagePath).toFile.delete()
          Paths.get(compressedImagePath).toFile.delete()

          Created(Json.toJson(id))
        }

      Ok("Image created")
    }

  def delete(id: Long): Action[AnyContent] = jwtAction.async { request =>
    imageRepository.delete(request.userId, id).flatMap {
      case Some(_) =>
        minioService.remove("images", id.toString) match {
          case Success(_) =>
            commentRepository.deleteByImageId(id).flatMap {
              case None =>
                Future.successful(NotFound(Json.toJson(id)))
              case Some(_) =>
                minioService.remove("compressed-images", id.toString) match {
                  case Success(_) =>
                    commentRepository.deleteByImageId(id).map {
                      case None =>
                        NotFound(Json.toJson(id))
                      case Some(_) =>
                        Accepted(Json.toJson(id))
                    }
                  case Failure(_) =>
                    Future.successful(NotFound(Json.toJson(id)))
                }
            }
          case Failure(_) => Future.successful(NotFound(Json.toJson(id)))
        }
      case None => Future.successful(NotFound(Json.toJson(id)))
    }
  }

  private def compressImageFile(
      imageFile: File,
      compressedPath: String
  ): String = {
    val image = ImmutableImage.loader().fromFile(imageFile)
    val writer = new JpegWriter().withCompression(30).withProgressive(true)
    image.output(writer, new File(compressedPath))
    compressedPath
  }
}
