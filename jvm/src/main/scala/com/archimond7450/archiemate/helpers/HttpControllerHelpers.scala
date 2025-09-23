package com.archimond7450.archiemate.helpers

import com.archimond7450.archiemate.extensions.Settings
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route

object HttpControllerHelpers {
  def failWithoutSessionCookie(
      innerRoute: HttpCookiePair => Route
  )(using settings: Settings): Route = {
    optionalCookie("session") {
      case Some(jwt) =>
        updateSessionCookie(jwt) { newJwt =>
          innerRoute(newJwt)
        }
      case None => complete(StatusCodes.Unauthorized)
    }
  }

  def logoutSessionCookie(
      innerRoute: => Route
  )(using settings: Settings): Route = {
    optionalCookie("session") { jwtOption =>
      expireOptionSessionCookie(jwtOption) {
        innerRoute
      }
    }
  }

  def expireSessionCookie(jwt: HttpCookiePair)(
      innerRoute: => Route
  ): Route = {
    setCookie(jwt.toCookie.withMaxAge(0)) {
      innerRoute
    }
  }

  def expireOptionSessionCookie(
      jwtOption: Option[HttpCookiePair]
  )(innerRoute: => Route)(using settings: Settings): Route = {
    jwtOption match {
      case Some(jwt) =>
        expireSessionCookie(jwt) {
          innerRoute
        }
      case None =>
        setCookie(
          HttpCookie("session", "")
            .withPath("/")
            .withSecure(settings.secureCookies)
            .withHttpOnly(true)
            .withMaxAge(0)
        ) {
          innerRoute
        }
    }
  }

  def createSessionCookie(jwt: String)(innerRoute: => Route)(using
      settings: Settings
  ): Route = {
    setCookie(
      HttpCookie("session", jwt)
        .withPath("/")
        .withSecure(settings.secureCookies)
        .withHttpOnly(true)
        .withMaxAge(settings.jwtExpiration.toSeconds)
    ) {
      innerRoute
    }
  }

  def updateSessionCookie(jwt: HttpCookiePair)(
      innerRoute: HttpCookiePair => Route
  )(using settings: Settings): Route = {
    val updatedCookie =
      jwt.toCookie.withMaxAge(settings.jwtExpiration.toSeconds)
    setCookie(
      updatedCookie
    ) {
      innerRoute(HttpCookiePair(updatedCookie.name, updatedCookie.value))
    }
  }
}
