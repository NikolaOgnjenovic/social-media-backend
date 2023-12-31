package dtos

import models.Image
import play.api.libs.json.{Json, Reads}

import scala.language.implicitConversions
case class NewImage(
    authorId: Long,
    tags: List[String],
    title: String
)

object NewImage {
  implicit val jsonReader: Reads[NewImage] = Json.reads[NewImage]

  implicit def toModel(newImage: NewImage): Image =
    Image(
      0,
      newImage.authorId,
      newImage.tags,
      newImage.title,
      0,
      List(
        newImage.authorId
      ),
      -1 // default folder id, handled on frontend
    )
}
