package auth

import play.api.mvc.{Request, WrappedRequest}

case class UserIdRequest[A](userId: Long, request: Request[A])
    extends WrappedRequest[A](request)
