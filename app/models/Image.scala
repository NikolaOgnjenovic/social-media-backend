package models
import play.api.libs.json.{Json, OFormat}
case class Image(
    id: Long,
    authorId: Long,
    tags: List[String],
    title: String,
    likes: Int
)

object Image {
  implicit val jsonFormat: OFormat[Image] = Json.format[Image]
}
