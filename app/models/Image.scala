package models
import play.api.libs.json.{Json, OFormat}
case class Image(
    id: Long,
    authorId: Long,
    tags: List[String],
    title: String,
    likeCount: Int,
    editorIds: List[Long], // A list of users which can edit the image data
    folderId: Long
)

object Image {
  implicit val jsonFormat: OFormat[Image] = Json.format[Image]
}
