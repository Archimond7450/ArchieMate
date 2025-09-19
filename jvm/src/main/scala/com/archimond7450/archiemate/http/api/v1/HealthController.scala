package com.archimond7450.archiemate.http.api.v1

import com.archimond7450.archiemate.http.IController
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route

class HealthController extends IController("health") {
  override def routes: Route = extractLog { log =>
    given LoggingAdapter = log
    liveness
  }

  private def liveness(using log: LoggingAdapter): Route = path("live") {
    log.debug("Liveness check")
    complete(StatusCodes.NoContent)
  }
}
