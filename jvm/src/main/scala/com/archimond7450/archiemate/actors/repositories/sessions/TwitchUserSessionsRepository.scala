package com.archimond7450.archiemate.actors.repositories.sessions

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.SerializerIDs
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.{
  GenericRepository,
  GenericSerializer
}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse.GetToken
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.scaladsl.Effect
import org.apache.pekko.serialization.Serializer

import java.io.NotSerializableException

object TwitchUserSessionsRepository {
  val actorName = "TwitchUserSessionsRepository"

  private case class State(
      users: Map[String, UserState] = Map.empty
  )
  private case class UserState(
      tokens: Map[String, GetToken] = Map.empty
  )

  sealed trait Command
  case class SetToken(tokenId: String, userId: String, token: GetToken)
      extends Command
  case class RefreshToken(tokenId: String, token: GetToken) extends Command
  case class GetTokenFromId(
      replyTo: ActorRef[ReturnedTokenFromId],
      tokenId: String
  ) extends Command
  case class GetTokenIdForUserId(
      replyTo: ActorRef[ReturnedTokenIdForUserId],
      userId: String
  ) extends Command

  case class ReturnedTokenFromId(token: Option[GetToken])
  case class ReturnedTokenIdForUserId(maybeTokenId: Option[String])

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class TokenSet(
      twitchTokenId: String,
      userId: String,
      token: GetToken
  ) extends Event
  private object TokenSet {
    given Decoder[TokenSet] = ConfiguredDecoder.derived
    given Encoder[TokenSet] = ConfiguredEncoder.derived
  }

  private final case class TokenRefreshed(
      twitchTokenId: String,
      token: GetToken
  ) extends Event
  private object TokenRefreshed {
    given Decoder[TokenRefreshed] = ConfiguredDecoder.derived
    given Encoder[TokenRefreshed] = ConfiguredEncoder.derived
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
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericRepository[Command, Event, State] {
      override protected val actorName: String =
        TwitchUserSessionsRepository.actorName
      override protected val emptyState: State = State()
      override protected val commandHandler
          : (State, Command) => Effect[Event, State] = { (state, command) =>
        command match {
          case SetToken(tokenId, userId, token) =>
            Effect.persist(TokenSet(tokenId, userId, token))

          case RefreshToken(tokenId, token) =>
            val userOption = state.users.find(_._2.tokens.contains(tokenId))
            if (userOption.nonEmpty) {
              Effect.persist(TokenRefreshed(tokenId, token))
            } else {
              Effect.none
            }

          case GetTokenFromId(replyTo, tokenId) =>
            val userOption = state.users.find(_._2.tokens.contains(tokenId))
            val userStateOption =
              userOption.flatMap(_._2.tokens.find(_._1 == tokenId))
            val tokenOption = userStateOption.map(_._2)
            replyTo ! ReturnedTokenFromId(tokenOption)
            Effect.none

          case GetTokenIdForUserId(replyTo, userId) =>
            val userStateOption = state.users.get(userId)
            val tokenOption =
              userStateOption.flatMap(_.tokens.find(_._2.scope.nonEmpty))
            replyTo ! ReturnedTokenIdForUserId(tokenOption.map(_._1))
            Effect.none
        }
      }

      override protected val eventHandler: (State, Event) => State = {
        (state, event) =>
          event match {
            case TokenSet(tokenId, userId, token) =>
              val userState = state.users.getOrElse(userId, UserState())
              updatedState(state, tokenId, userId, token, userState)

            case TokenRefreshed(tokenId, token) =>
              val (userId, userState) =
                state.users.find(_._2.tokens.contains(tokenId)).get
              updatedState(state, tokenId, userId, token, userState)
          }
      }

      override protected val onRecoveryCompleted: State => Unit =
        _.users.foreach { (userId, userState) =>
          val tokenId = userState.tokens
            .find(_._2.scope.nonEmpty)
            .getOrElse(userState.tokens.last)
            ._1
          mediator ! ArchieMateMediator.SendTwitchApiClientCommand(
            TwitchApiClient.GetTokenUserFromTokenId(
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

      def updatedState(
          state: State,
          tokenId: String,
          userId: String,
          token: GetToken,
          userState: UserState
      ): State = {
        val newTokens = userState.tokens + (tokenId -> token)
        val newUserState = UserState(newTokens)
        State(state.users + (userId -> newUserState))
      }
    }.eventSourcedBehavior()
  }
}
