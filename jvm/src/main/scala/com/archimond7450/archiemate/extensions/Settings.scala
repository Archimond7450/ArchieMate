package com.archimond7450.archiemate.extensions

import com.typesafe.config.Config
import org.apache.pekko.actor
import org.apache.pekko.actor.{ClassicActorSystemProvider, ExtensionIdProvider}
import org.apache.pekko.actor.typed.{ActorSystem, Extension, ExtensionId}
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.util.Timeout

import java.util.concurrent.TimeUnit
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class Settings(config: Config) extends Extension {
  val askTimeout: Timeout = Timeout.durationToTimeout(FiniteDuration(config.getDuration("archiemate.ask-timeout", TimeUnit.NANOSECONDS), duration.NANOSECONDS))
  val jwtSecret: String = config.getString("archiemate.jwt.secret")
  val jwtExpiration: FiniteDuration = FiniteDuration(config.getDuration("archiemate.jwt.expiration", TimeUnit.NANOSECONDS), duration.NANOSECONDS)
  val jwtIssuer: String = config.getString("archiemate.jwt.issuer")
  val httpInterface: String = config.getString("archiemate.http.interface")
  val httpPort: Int = config.getInt("archiemate.http.port")
  val archiemateRedirectUriPrefix: String = config.getString("archiemate.http.redirect-uri-prefix")
  val secureCookies: Boolean = archiemateRedirectUriPrefix.startsWith("https://")
  val newLoginExpirationTime: FiniteDuration = FiniteDuration(config.getDuration("archiemate.new-login.expiration-time", TimeUnit.NANOSECONDS), duration.NANOSECONDS)
  val twitchIRCUsername: String = config.getString("archiemate.twitch.irc.username")
  val twitchIRCToken: String = config.getString("archiemate.twitch.irc.token")
  val twitchIRCUri: Uri = Uri(config.getString("archiemate.twitch.irc.uri"))
  val twitchEventSubUri: Uri = Uri(config.getString("archiemate.twitch.eventsub.uri"))
  val twitchEventSubKeepalive: FiniteDuration = FiniteDuration(config.getDuration("archiemate.twitch.eventsub.keepalive", TimeUnit.SECONDS), duration.SECONDS)
  val twitchAppClientId: String = config.getString("archiemate.twitch.app.client-id")
  val twitchAppClientSecret: String = config.getString("archiemate.twitch.app.client-secret")
  val twitchAppRedirectUri: String = archiemateRedirectUriPrefix + config.getString("archiemate.twitch.app.redirect-uri-postfix")
  val youTubeAppClientId: String = config.getString("archiemate.youtube.app.client-id")
  val youTubeAppClientSecret: String = config.getString("archiemate.youtube.app.client-secret")
  val youTubeAppRedirectUri: String = archiemateRedirectUriPrefix + config.getString("archiemate.youtube.app.redirect-uri-postfix")
}

object Settings extends ExtensionId[Settings] {
  def createExtension(system: ActorSystem[_]): Settings = new Settings(system.settings.config)
}
