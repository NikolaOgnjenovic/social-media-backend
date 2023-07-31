package services
import com.google.inject.Inject

import java.time.Clock
import pdi.jwt._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import repositories.JwtRepository
import models.JwtToken

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class JwtService @Inject() (
    jwtRepository: JwtRepository,
    implicit val ec: ExecutionContext
) {
  implicit val clock: Clock = Clock.systemUTC
  private val encryptionKey = "mrmi"
  private val algorithm = JwtAlgorithm.HS256

  // TODO: When blacklisting a token new ones that get generated fail because they are blacklisted.
  //  I suppose that implies that tokens are indeed the same for each userId with the given encryption key and algorithm
  //  Why would I blacklist then? And how would I generate a new one for the same user?
  def generateToken(userId: Long): Future[String] = {
    val claim = Json.obj(("userId", userId))
    val token = JwtJson.encode(claim, encryptionKey, algorithm)
    Future.successful(token)
  }

  // Checks if the token is validly encrypted with the default encryption key and algortihm
  def isValid(token: String): Boolean = {
    JwtJson.isValid(token, encryptionKey, Seq(algorithm))
  }

  // Reads the user id from the given JwtToken.
  def getUserIdFromToken(token: String): Option[Long] = {
    val tryClaims: Try[JwtClaim] =
      Jwt.decode(token, encryptionKey, Seq(algorithm))
    val jsValueOption: Option[JsValue] =
      tryClaims.toOption.map(claims => Json.parse(claims.content))
    val userIdOption: Option[Long] =
      jsValueOption.flatMap(jsValue => (jsValue \ "userId").asOpt[Long])
    userIdOption
  }

  def blacklistToken(token: String): Future[Option[String]] = {
    jwtRepository.getAll.flatMap { blacklistedTokens =>
      if (blacklistedTokens.contains(token)) {
        // Token is already blacklisted, return None
        Future.successful(None)
      } else {
        // Token is not blacklisted, insert it and return Some(token)
        jwtRepository.blacklist(token).map(_ => Some(token))
      }
    }
  }
}
