package com.archimond7450.archiemate.actors.services.caches

import com.archimond7450.archiemate.kick.api.KickApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}

object KickTokenUserCacheService {
  val actorName = "KickTokenUserCacheService"

  sealed trait Command
  final case class CacheTokenUser(tokenUser: KickApiResponse.User)
      extends Command
  final case class GetTokenUserFromUserId(
      replyTo: ActorRef[Option[KickApiResponse.User]],
      userId: Int
  ) extends Command
  final case class GetTokenUserFromUserName(
      replyTo: ActorRef[Option[KickApiResponse.User]],
      userName: String
  ) extends Command

  def apply(): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx

        ready()
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)

  private def ready(tokenUsers: Map[Int, KickApiResponse.User] = Map.empty)(
      using ActorContext[Command]
  ): Behavior[Command] = Behaviors.logMessages {
    Behaviors.receiveMessage {
      case CacheTokenUser(tokenUser) =>
        ready(tokenUsers + (tokenUser.userId -> tokenUser))

      case GetTokenUserFromUserId(replyTo, userId) =>
        replyTo ! tokenUsers.get(userId)
        Behaviors.same

      case GetTokenUserFromUserName(replyTo, userName) =>
        replyTo ! tokenUsers.find(_._2.name == userName).map(_._2)
        Behaviors.same
    }
  }
}
