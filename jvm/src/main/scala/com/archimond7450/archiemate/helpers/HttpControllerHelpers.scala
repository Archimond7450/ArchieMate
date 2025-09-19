package com.archimond7450.archiemate.helpers

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.HttpCookiePair
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route

object HttpControllerHelpers {
  def failWithoutSessionCookie(route: HttpCookiePair => Route): Route = optionalCookie("session") {
    case Some(jwt) => route(jwt)
    case None => complete(StatusCodes.Unauthorized)
  }
}
