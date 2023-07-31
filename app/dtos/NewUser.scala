package dtos

import models.User
import play.api.libs.json.{Json, Reads}

import scala.language.implicitConversions

case class NewUser(username: String, password: String)

object NewUser {
  implicit val jsonReader: Reads[NewUser] = Json.reads[NewUser]

  implicit def toModel(newUser: NewUser): User =
    User(0, newUser.username, newUser.password, List(), List())
}
