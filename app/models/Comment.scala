package models

import play.api.libs.json.{Json, OFormat}

case class Comment(
    id: Long,
    authorId: Long,
    imageId: Long,
    content: String,
    likeCount: Int
)

object Comment {
  implicit val jsonFormat: OFormat[Comment] = Json.format[Comment]
}
