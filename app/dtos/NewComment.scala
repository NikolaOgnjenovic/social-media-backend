package dtos

import models.Comment
import play.api.libs.json.{Json, Reads}
import scala.language.implicitConversions

case class NewComment(authorId: Long, imageId: Long, content: String)

object NewComment {
  implicit val jsonReader: Reads[NewComment] = Json.reads[NewComment]

  implicit def toModel(newComment: NewComment): Comment =
    Comment(0, newComment.authorId, newComment.imageId, newComment.content, 0)
}
