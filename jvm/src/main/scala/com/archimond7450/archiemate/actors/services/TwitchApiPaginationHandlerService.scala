package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success, Try}

object TwitchApiPaginationHandlerService {
  val actorName = "TwitchApiPaginationHandlerService"

  sealed trait Command

  final case class GetChatters(clientCmd: TwitchApiClient.GetChatters)
      extends Command
  final case class GetModerators(clientCmd: TwitchApiClient.GetModerators)
      extends Command
  final case class GetVIPs(clientCmd: TwitchApiClient.GetVIPs) extends Command
  final case class GetSubs(clientCmd: TwitchApiClient.GetSubs) extends Command
  final case class GetChannelFollowers(
      clientCmd: TwitchApiClient.GetChannelFollowers
  ) extends Command
  final case class GetStream(clientCmd: TwitchApiClient.GetStream)
      extends Command
  final case class GetPolls(clientCmd: TwitchApiClient.GetPolls) extends Command
  final case class GetPredictions(clientCmd: TwitchApiClient.GetPredictions)
      extends Command

  private final case class ChattersResponse(
      cmd: GetChatters,
      tryChatters: Try[TwitchApiResponse.GetChatters]
  ) extends Command
  private final case class ModeratorsResponse(
      cmd: GetModerators,
      tryModerators: Try[TwitchApiResponse.GetModerators]
  ) extends Command
  private final case class VIPsResponse(
      cmd: GetVIPs,
      tryVips: Try[TwitchApiResponse.GetVIPs]
  ) extends Command
  private final case class SubsResponse(
      cmd: GetSubs,
      trySubs: Try[TwitchApiResponse.GetSubs]
  ) extends Command
  private final case class ChannelFollowersResponse(
      cmd: GetChannelFollowers,
      tryChannelFollowers: Try[TwitchApiResponse.GetChannelFollowers]
  ) extends Command
  private final case class StreamResponse(
      cmd: GetStream,
      tryStream: Try[TwitchApiResponse.GetStream]
  ) extends Command
  private final case class PollsResponse(
      cmd: GetPolls,
      tryPolls: Try[TwitchApiResponse.GetPolls]
  ) extends Command
  private final case class PredictionsResponse(
      cmd: GetPredictions,
      tryPredictions: Try[TwitchApiResponse.GetPredictions]
  ) extends Command

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      timeout: Timeout
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx

        new TwitchApiPaginationHandlerService().active()
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}

class TwitchApiPaginationHandlerService(using
    ctx: ActorContext[TwitchApiPaginationHandlerService.Command],
    timeout: Timeout,
    mediator: ActorRef[ArchieMateMediator.Command]
) {
  import TwitchApiPaginationHandlerService.*
  private case class State(
      chatters: Map[GetChatters, List[TwitchApiResponse.GetChatters]] =
        Map.empty,
      moderators: Map[GetModerators, List[TwitchApiResponse.GetModerators]] =
        Map.empty,
      vips: Map[GetVIPs, List[TwitchApiResponse.GetVIPs]] = Map.empty,
      subs: Map[GetSubs, List[TwitchApiResponse.GetSubs]] = Map.empty,
      followers: Map[GetChannelFollowers, List[
        TwitchApiResponse.GetChannelFollowers
      ]] = Map.empty,
      stream: Map[GetStream, List[TwitchApiResponse.GetStream]] = Map.empty,
      polls: Map[GetPolls, List[TwitchApiResponse.GetPolls]] = Map.empty,
      predictions: Map[GetPredictions, List[TwitchApiResponse.GetPredictions]] =
        Map.empty
  )

  private def active(
      state: State = State()
  )(using
      ctx: ActorContext[Command],
      timeout: Timeout,
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case cmd @ GetChatters(clientCmd) =>
      askForChatters(cmd)
      active(state.copy(chatters = state.chatters + (cmd -> Nil)))
    case cmd @ GetModerators(clientCmd) =>
      askForModerators(cmd)
      active(state.copy(moderators = state.moderators + (cmd -> Nil)))
    case cmd @ GetVIPs(clientCmd) =>
      askForVIPs(cmd)
      active(state.copy(vips = state.vips + (cmd -> Nil)))
    case cmd @ GetSubs(clientCmd) =>
      askForSubs(cmd)
      active(state.copy(subs = state.subs + (cmd -> Nil)))
    case cmd @ GetChannelFollowers(clientCmd) =>
      askForChannelFollowers(cmd)
      active(state.copy(followers = state.followers + (cmd -> Nil)))
    case cmd @ GetStream(clientCmd) =>
      askForStream(cmd)
      active(state.copy(stream = state.stream + (cmd -> Nil)))
    case cmd @ GetPolls(clientCmd) =>
      askForPolls(cmd)
      active(state.copy(polls = state.polls + (cmd -> Nil)))
    case cmd @ GetPredictions(clientCmd) =>
      askForPredictions(cmd)
      active(state.copy(predictions = state.predictions + (cmd -> Nil)))
    case ChattersResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(chatters = state.chatters - cmd))
    case ModeratorsResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(moderators = state.moderators - cmd))
    case VIPsResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(vips = state.vips - cmd))
    case SubsResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(subs = state.subs - cmd))
    case ChannelFollowersResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(followers = state.followers - cmd))
    case StreamResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(stream = state.stream - cmd))
    case PollsResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(polls = state.polls - cmd))
    case PredictionsResponse(cmd, Failure(ex)) =>
      cmd.clientCmd.replyTo ! StatusReply.error(ex)
      active(state.copy(predictions = state.predictions - cmd))
    case ChattersResponse(cmd, Success(chatters)) =>
      chatters.pagination.cursor match {
        case Some(cursor) =>
          askForChatters(cmd, cursor = Some(cursor))
          active(
            state.copy(chatters =
              state.chatters + (cmd -> (state.chatters(cmd) :+ chatters))
            )
          )
        case _ =>
          val chattersData = state.chatters(cmd) :+ chatters
          val finalData = chattersData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            chatters.copy(data = finalData.toList)
          )
          active(state.copy(chatters = state.chatters - cmd))
      }
    case ModeratorsResponse(cmd, Success(moderators)) =>
      moderators.pagination.cursor match {
        case Some(cursor) =>
          askForModerators(cmd, cursor = Some(cursor))
          active(
            state.copy(moderators =
              state.moderators + (cmd -> (state.moderators(cmd) :+ moderators))
            )
          )
        case _ =>
          val moderatorsData = state.moderators(cmd) :+ moderators
          val finalData = moderatorsData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            moderators.copy(data = finalData.toList)
          )
          active(state.copy(moderators = state.moderators - cmd))
      }
    case VIPsResponse(cmd, Success(vips)) =>
      vips.pagination.cursor match {
        case Some(cursor) =>
          askForVIPs(cmd, cursor = Some(cursor))
          active(
            state.copy(vips = state.vips + (cmd -> (state.vips(cmd) :+ vips)))
          )
        case _ =>
          val vipsData = state.vips(cmd) :+ vips
          val finalData = vipsData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            vips.copy(data = finalData.toList)
          )
          active(state.copy(vips = state.vips - cmd))
      }
    case SubsResponse(cmd, Success(subs)) =>
      subs.pagination.cursor match {
        case Some(cursor)
            if subs.data.length + state
              .subs(cmd)
              .map(_.data.length)
              .sum <= subs.total =>
          askForSubs(cmd, cursor = Some(cursor))
          active(
            state.copy(subs = state.subs + (cmd -> (state.subs(cmd) :+ subs)))
          )
        case _ =>
          val subsData = state.subs(cmd) :+ subs
          val finalData = subsData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            subs.copy(data = finalData.toList)
          )
          active(state.copy(subs = state.subs - cmd))
      }
    case ChannelFollowersResponse(cmd, Success(followers)) =>
      followers.pagination.cursor match {
        case Some(cursor) =>
          askForChannelFollowers(cmd, cursor = Some(cursor))
          active(
            state.copy(followers =
              state.followers + (cmd -> (state.followers(cmd) :+ followers))
            )
          )
        case _ =>
          val followersData = state.followers(cmd) :+ followers
          val finalData = followersData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            followers.copy(data = finalData.toList)
          )
          active(state.copy(followers = state.followers - cmd))
      }
    case StreamResponse(cmd, Success(stream)) =>
      stream.pagination.cursor match {
        case Some(cursor) =>
          askForStream(cmd, cursor = Some(cursor))
          active(
            state.copy(stream =
              state.stream + (cmd -> (state.stream(cmd) :+ stream))
            )
          )
        case _ =>
          val streamData = state.stream(cmd) :+ stream
          val finalData = streamData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            stream.copy(data = finalData.toList)
          )
          active(state.copy(stream = state.stream - cmd))
      }
    case PollsResponse(cmd, Success(polls)) =>
      polls.pagination.cursor match {
        case Some(cursor) =>
          askForPolls(cmd, cursor = Some(cursor))
          active(
            state.copy(polls =
              state.polls + (cmd -> (state.polls(cmd) :+ polls))
            )
          )
        case _ =>
          val pollsData = state.polls(cmd) :+ polls
          val finalData = pollsData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            polls.copy(data = finalData.toList)
          )
          active(state.copy(polls = state.polls - cmd))
      }
    case PredictionsResponse(cmd, Success(predictions)) =>
      predictions.pagination.cursor match {
        case Some(cursor) =>
          askForPredictions(cmd, cursor = Some(cursor))
          active(
            state.copy(predictions =
              state.predictions + (cmd -> (state.predictions(
                cmd
              ) :+ predictions))
            )
          )
        case _ =>
          val predictionsData = state.predictions(cmd) :+ predictions
          val finalData = predictionsData.flatMap(_.data).toSet
          cmd.clientCmd.replyTo ! StatusReply.success(
            predictions.copy(data = finalData.toList)
          )
          active(state.copy(predictions = state.predictions - cmd))
      }
  }

  private def askForChatters(
      cmd: GetChatters,
      cursor: Option[String] = None
  ): Unit = {
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      TwitchApiResponse.GetChatters
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(ChattersResponse(cmd, _))
  }

  private def askForModerators(
      cmd: GetModerators,
      cursor: Option[String] = None
  ): Unit = {
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      TwitchApiResponse.GetModerators
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(ModeratorsResponse(cmd, _))
  }

  private def askForVIPs(cmd: GetVIPs, cursor: Option[String] = None): Unit = {
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetVIPs](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(VIPsResponse(cmd, _))
  }

  private def askForSubs(cmd: GetSubs, cursor: Option[String] = None): Unit = {
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetSubs](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(SubsResponse(cmd, _))
  }

  private def askForChannelFollowers(
      cmd: GetChannelFollowers,
      cursor: Option[String] = None
  ): Unit = {
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      TwitchApiResponse.GetChannelFollowers
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(ChannelFollowersResponse(cmd, _))
  }

  private def askForStream(
      cmd: GetStream,
      cursor: Option[String] = None
  ): Unit = {
    ctx
      .askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetStream](
        mediator,
        ref =>
          ArchieMateMediator.SendTwitchApiClientCommand(
            cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
          )
      )(StreamResponse(cmd, _))
  }

  private def askForPolls(
      cmd: GetPolls,
      cursor: Option[String] = None
  ): Unit = {
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetPolls](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(PollsResponse(cmd, _))
  }

  private def askForPredictions(
      cmd: GetPredictions,
      cursor: Option[String] = None
  ): Unit = {
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      TwitchApiResponse.GetPredictions
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          cmd.clientCmd.copy(replyTo = ref, cursor = cursor)
        )
    )(PredictionsResponse(cmd, _))
  }
}
