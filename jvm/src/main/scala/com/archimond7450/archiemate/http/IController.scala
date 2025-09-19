package com.archimond7450.archiemate.http

import org.apache.pekko.http.scaladsl.server.Directives.pathPrefix
import org.apache.pekko.http.scaladsl.server.{PathMatcher, Route}

abstract class IController(private val baseEndpoint: PathMatcher[Unit]) {
  def getAllRoutes: Route = pathPrefix(baseEndpoint) {
    routes
  }

  def routes: Route
}
