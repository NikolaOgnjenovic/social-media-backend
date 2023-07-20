package services
import com.google.inject.Inject

import java.time.Clock
import pdi.jwt._
import play.api.libs.json.Json
import play.api.libs.json.JsValue

import scala.util.Try

class JwtService @Inject() () {
  implicit val clock: Clock = Clock.systemUTC
  private val encryptionKey = "mrmi"
  private val algorithm =
    JwtAlgorithm.HS256 //  TODO: when sending with an OK(), the key gets too long if it isn't hdm5. Solve this??

  def generateToken(userId: Long): String = {
    val claim = Json.obj(("userId", userId))

    JwtJson.encode(claim, encryptionKey, algorithm)
  }

  def isValid(token: String): Boolean = {
    JwtJson.isValid(token, encryptionKey, Seq(algorithm))
  }

  def getUserIdFromToken(token: String): Option[Long] = {
    val tryClaims: Try[JwtClaim] =
      Jwt.decode(token, encryptionKey, Seq(algorithm))
    val jsValueOption: Option[JsValue] =
      tryClaims.toOption.map(claims => Json.parse(claims.content))
    val userIdOption: Option[Long] =
      jsValueOption.flatMap(jsValue => (jsValue \ "userId").asOpt[Long])
    userIdOption
  }
}
