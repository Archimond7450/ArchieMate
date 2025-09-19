package com.archimond7450.archiemate.actors.services.caches

import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object TwitchTokenUserCacheService {
  val actorName = "TwitchTokenUserCacheService"

  sealed trait Command
  final case class CacheTokenUser(tokenUser: TwitchApiResponse.GetTokenUser) extends Command
  final case class GetTokenUserFromUserId(replyTo: ActorRef[Option[TwitchApiResponse.GetTokenUser]], userId: String) extends Command
  final case class GetTokenUserFromUserName(replyTo: ActorRef[Option[TwitchApiResponse.GetTokenUser]], userName: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    ready()
  }

  private def ready(tokenUsers: Map[String, TwitchApiResponse.GetTokenUser] = Map.empty)(using ActorContext[Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case CacheTokenUser(tokenUser) =>
      ready(tokenUsers + (tokenUser.id -> tokenUser))

    case GetTokenUserFromUserId(replyTo, userId) =>
      replyTo ! tokenUsers.get(userId)
      Behaviors.same

    case GetTokenUserFromUserName(replyTo, userName) =>
      replyTo ! tokenUsers.find(_._2.login == userName).map(_._2)
      Behaviors.same
  }
}
