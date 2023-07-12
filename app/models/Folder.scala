package models
import play.api.libs.json.{Json, OFormat}

case class Folder(id: Long, authorId: Long, imageIds: String, title: String)

object Folder {
  implicit val jsonFormat: OFormat[Folder] = Json.format[Folder]
}
