package dtos

import models.Image
import play.api.libs.json.{Json, Reads}
import scala.language.implicitConversions

case class NewImage(title: String)

object NewImage {
  implicit val jsonReader: Reads[NewImage] = Json.reads[NewImage]
  implicit def toModel(newImage: NewImage): Image = Image(0, newImage.title)
}
