package com.archimond7450.archiemate.actors.repositories.sessions

import com.archimond7450.archiemate.CirceConfiguration.youtubeConfiguration
import com.archimond7450.archiemate.SerializerIDs
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.GenericSerializer
import com.archimond7450.archiemate.actors.youtube.api.YouTubeApiClient
import com.archimond7450.archiemate.youtube.api.YouTubeApiResponse.GetToken
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.{PersistenceId, RecoveryCompleted}
import org.apache.pekko.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}

object YouTubeChannelSessionsRepository {
  val actorName = "YouTubeChannelSessionsRepository"

  private case class State(users: Map[String, UserState] = Map.empty)
  private case class UserState(tokens: Map[String, GetToken] = Map.empty)

  sealed trait Command
  case class SetToken(tokenId: String, userId: String, token: GetToken)
      extends Command
  case class RefreshToken(tokenId: String, token: GetToken) extends Command
  case class GetTokenFromId(
      replyTo: ActorRef[ReturnedTokenFromId],
      tokenId: String
  ) extends Command
  case class GetTokenIdsForUserId(
      replyTo: ActorRef[ReturnedTokenIdsForUserId],
      userId: String
  ) extends Command

  case class ReturnedTokenFromId(id: String, token: Option[GetToken])
  case class ReturnedTokenIdsForUserId(userId: String, tokenIds: List[String])

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class TokenSet(
      youtubeTokenId: String,
      userId: String,
      token: GetToken
  ) extends Event
  private object TokenSet {
    given Decoder[TokenSet] = ConfiguredDecoder.derived
    given Encoder[TokenSet] = ConfiguredEncoder.derived
  }

  private final case class TokenRefreshed(
      youtubeTokenId: String,
      token: GetToken
  ) extends Event
  private object TokenRefreshed {
    given Decoder[TokenRefreshed] = ConfiguredDecoder.derived
    given Encoder[TokenRefreshed] = ConfiguredEncoder.derived
  }

  private class EventSerializer
      extends GenericSerializer[Event](
        actorName,
        SerializerIDs.youTubeChannelSessionsRepositoryId
      ) {

    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] = Behaviors.setup { ctx =>
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
          mediator ! ArchieMateMediator.SendYouTubeApiClientCommand(
            YouTubeApiClient.GetChannelFromTokenId(
              ctx.system.ignoreRef,
              tokenId
            )
          )
          ctx.log.debug(
            "Sent GetChannelFromTokenId to youTubeApiClient for userId {} and tokenId {}",
            userId,
            tokenId
          )
        }
      }
  }

  private val commandHandler: (State, Command) => ReplyEffect[Event, State] = {
    (state, command) =>
      command match {
        case SetToken(tokenId, userId, token) =>
          Effect.persist(TokenSet(tokenId, userId, token)).thenNoReply()

        case RefreshToken(tokenId, token) =>
          val userOption = state.users.find(_._2.tokens.contains(tokenId))
          if (userOption.nonEmpty) {
            Effect.persist(TokenRefreshed(tokenId, token)).thenNoReply()
          } else {
            Effect.none.thenNoReply()
          }

        case GetTokenFromId(replyTo, tokenId) =>
          Effect.none.thenReply(replyTo) { state =>
            val userOption = state.users.find(_._2.tokens.contains(tokenId))
            val userStateOption =
              userOption.flatMap(_._2.tokens.find(_._1 == tokenId))
            val tokenOption = userStateOption.map(_._2)
            ReturnedTokenFromId(tokenId, tokenOption)
          }

        case GetTokenIdsForUserId(replyTo, userId) =>
          Effect.none.thenReply(replyTo) { state =>
            val userStateOption = state.users.get(userId)
            val tokenIdsOption = userStateOption.map(_.tokens.keys.toList)
            ReturnedTokenIdsForUserId(
              userId,
              tokenIdsOption.getOrElse(Nil)
            )
          }
      }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
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

  private def updatedState(
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
}
