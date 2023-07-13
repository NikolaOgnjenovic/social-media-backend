package dtos

import models.Folder
import play.api.libs.json.{Json, Reads}
import scala.language.implicitConversions

case class NewFolder(authorId: Long, title: String)

object NewFolder {
  implicit val jsonReader: Reads[NewFolder] = Json.reads[NewFolder]

  implicit def toModel(newFolder: NewFolder): Folder =
    Folder(0, newFolder.authorId, newFolder.title)
}
