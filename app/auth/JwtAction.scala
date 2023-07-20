package auth

import play.api.mvc.Results.{Forbidden, NotFound, Unauthorized}
import play.api.mvc._
import services.JwtService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JwtAction @Inject() (jwtService: JwtService)(implicit
    cc: ControllerComponents
) extends ActionBuilder[UserIdRequest, AnyContent] {
  override def invokeBlock[A](
      request: Request[A],
      block: UserIdRequest[A] => Future[Result]
  ): Future[Result] = {
    extractJwtToken(request) match {
      case Some(jwtToken) =>
        // Validate the JWT token and extract the user ID
        if (jwtService.isValid(jwtToken)) {
          val userIdOption = jwtService.getUserIdFromToken(jwtToken)
          userIdOption match {
            case Some(userId) =>
              block(auth.UserIdRequest(userId, request))
            case None =>
              Future.successful(NotFound("User id not found in token"))
          }
        } else {
          Future.successful(Forbidden("Invalid JWT token"))
        }

      case None =>
        Future.successful(Unauthorized("JWT token not provided"))
    }
  }

  private def extractJwtToken(request: RequestHeader): Option[String] = {
    request.headers.get("Authorization").flatMap { headerValue =>
      if (headerValue.startsWith("Bearer ")) {
        Some(
          headerValue.substring(7)
        ) // Remove "Bearer " prefix to get the token
      } else {
        None // Token not found in the request
      }
    }
  }

  override protected def executionContext: ExecutionContext =
    cc.executionContext

  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
}
