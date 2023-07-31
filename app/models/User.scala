package models

import play.api.libs.json.{Json, OFormat}

case class User(
    id: Long,
    username: String,
    password: String,
    likedImageIds: List[Long],
    likedCommentIds: List[Long]
)
object User {
  implicit val jsonFormat: OFormat[User] = Json.format[User]
}
