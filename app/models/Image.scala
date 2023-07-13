package models
import play.api.libs.json.{Json, OFormat}
case class Image(
    id: Long,
    authorId: Long,
    tags: List[String],
    title: String,
    likeCount: Int,
    editorIds: List[Long], // A list of users which can edit the image data
    folderId: Long,
    imagePath: String
    // TODO: Where do I authenticate? On the front end?
    //  What if someone sends a request to edit a random image? How do I check if the user is in the editors list
)

object Image {
  implicit val jsonFormat: OFormat[Image] = Json.format[Image]
}
