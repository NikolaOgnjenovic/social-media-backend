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
  private val algorithm =
    JwtAlgorithm.HS256 //  TODO: when sending with an OK(), the key gets too long if it isn't hdm5. Solve this??

//  def generateToken(userId: Long, maxRetries: Int = 20): Future[String] = {
//    val claim = Json.obj(("userId", userId))
//    val token = JwtJson.encode(claim, encryptionKey, algorithm)
//    jwtRepository.getAll.flatMap { blacklistedTokens =>
//      if (blacklistedTokens.contains(token)) {
//        if (maxRetries > 0) {
//          generateToken(userId, maxRetries - 1)
//        } else {
//          // Maximum retries reached, handle the error or return a default token
//          Future.failed(
//            new Exception(
//              "Maximum retries reached. Unable to generate a non-blacklisted token."
//            )
//          )
//        }
//      } else {
//        Future.successful(token)
//      }
//    }
//  }
  def generateToken(userId: Long): Future[String] = {
    val claim = Json.obj(("userId", userId))
    val token = JwtJson.encode(claim, encryptionKey, algorithm)
    Future.successful(token)
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
