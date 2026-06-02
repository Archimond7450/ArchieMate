package com.archimond7450.archiemate.actors.repositories.sessions

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.SerializerIDs
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.GenericSerializer
import com.archimond7450.archiemate.actors.kick.api.KickApiClient
import com.archimond7450.archiemate.kick.api.KickApiResponse.GetToken
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}
import org.apache.pekko.persistence.typed.{PersistenceId, RecoveryCompleted}

object KickUserSessionsRepository {
  val actorName = "KickUserSessionsRepository"

  private case class State(
      users: Map[String, UserState] = Map.empty
  )
  private case class UserState(
      tokens: Map[String, GetToken] = Map.empty
  )

  sealed trait Command
  case class SetToken(tokenId: String, twitchUserId: String, token: GetToken)
      extends Command
  case class RefreshToken(tokenId: String, token: GetToken) extends Command
  case class GetTokenFromId(
      replyTo: ActorRef[ReturnedTokenFromId],
      tokenId: String
  ) extends Command
  case class GetTokenIdForTwitchUserId(
      replyTo: ActorRef[ReturnedTokenIdForTwitchUserId],
      twitchUserId: String
  ) extends Command

  case class ReturnedTokenFromId(token: Option[GetToken])
  case class ReturnedTokenIdForTwitchUserId(maybeTokenId: Option[String])

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class KickTokenSet(
      kickTokenId: String,
      twitchUserId: String,
      token: GetToken
  ) extends Event
  private object KickTokenSet {
    given Decoder[KickTokenSet] = ConfiguredDecoder.derived
    given Encoder[KickTokenSet] = ConfiguredEncoder.derived
  }

  private final case class KickTokenRefreshed(
      kickTokenId: String,
      token: GetToken
  ) extends Event
  private object KickTokenRefreshed {
    given Decoder[KickTokenRefreshed] = ConfiguredDecoder.derived
    given Encoder[KickTokenRefreshed] = ConfiguredEncoder.derived
  }

  private class EventSerializer
      extends GenericSerializer[Event](
        actorName,
        SerializerIDs.twitchUserSessionsRepositoryId
      ) {

    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        EventSourcedBehavior
          .withEnforcedReplies[Command, Event, State](
            persistenceId = PersistenceId.ofUniqueId(actorName),
            emptyState = State(),
            commandHandler = commandHandler,
            eventHandler = eventHandler
          )
          .receiveSignal { case (state, RecoveryCompleted) =>
            state.users.foreach { (userId, userState) =>
              val tokenId = userState.tokens
                .find(_._2.scope.nonEmpty)
                .getOrElse(userState.tokens.last)
                ._1
              mediator ! ArchieMateMediator.SendKickApiClientCommand(
                KickApiClient.GetTokenUserFromTokenId(
                  ctx.system.ignoreRef,
                  tokenId
                )
              )
              ctx.log.debug(
                "Sent GetTokenUserFromTokenId to twitchApiClient for userId {} and tokenId {}",
                userId,
                tokenId
              )
            }
          }
      }
    }
    .onFailure[Throwable](SupervisorStrategy.restart)

  private val commandHandler: (State, Command) => ReplyEffect[Event, State] = {
    (state, command) =>
      command match {
        case SetToken(tokenId, twitchUserId, token) =>
          Effect.persist(KickTokenSet(tokenId, twitchUserId, token)).thenNoReply()

        case RefreshToken(tokenId, token) =>
          val userOption = state.users.find(_._2.tokens.contains(tokenId))
          if (userOption.nonEmpty) {
            Effect.persist(KickTokenRefreshed(tokenId, token)).thenNoReply()
          } else {
            Effect.none.thenNoReply()
          }

        case GetTokenFromId(replyTo, tokenId) =>
          Effect.none.thenReply(replyTo) { state =>
            val userOption = state.users.find(_._2.tokens.contains(tokenId))
            val userStateOption =
              userOption.flatMap(_._2.tokens.find(_._1 == tokenId))
            val tokenOption = userStateOption.map(_._2)
            ReturnedTokenFromId(tokenOption)
          }

        case GetTokenIdForTwitchUserId(replyTo, twitchUserId) =>
          Effect.none.thenReply(replyTo) { state =>
            val userStateOption = state.users.get(twitchUserId)
            val tokenOption =
              userStateOption.flatMap(_.tokens.find(_._2.scope.nonEmpty))
            ReturnedTokenIdForTwitchUserId(tokenOption.map(_._1))
          }
      }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case KickTokenSet(tokenId, userId, token) =>
        val userState = state.users.getOrElse(userId, UserState())
        updatedState(state, tokenId, userId, token, userState)

      case KickTokenRefreshed(tokenId, token) =>
        val (userId, userState) =
          state.users.find(_._2.tokens.contains(tokenId)).get
        updatedState(state, tokenId, userId, token, userState)
    }
  }

  private def updatedState(
      state: State,
      tokenId: String,
      twitchUserId: String,
      token: GetToken,
      userState: UserState
  ): State = {
    val newTokens = userState.tokens + (tokenId -> token)
    val newUserState = UserState(newTokens)
    State(state.users + (twitchUserId -> newUserState))
  }
}
