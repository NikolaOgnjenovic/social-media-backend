package models

import play.api.libs.json.{Json, OFormat}
case class Image(id: Long, title: String)

object Image {
  implicit val jsonFormat: OFormat[Image] = Json.format[Image]
}
