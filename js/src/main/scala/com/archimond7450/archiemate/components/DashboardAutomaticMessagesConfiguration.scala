package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.elements.StyledStandardElements.{
  h2Element,
  pElement
}
import com.archimond7450.archiemate.helpers.FetchHelpers.{
  fetchGetStream,
  fetchPostStream
}
import com.archimond7450.archiemate.http.ChannelSettings.{
  AutomaticMessagesSettings,
  KnownGreetsSettings
}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardAutomaticMessagesConfiguration {
  final val automaticMessagesSettingsEndpoint =
    "/api/v1/settings/automatic_messages"

  def render(): ReactiveHtmlElement[HTMLDivElement] = {
    val automaticMessagesStream = fetchGetStream[AutomaticMessagesSettings](
      automaticMessagesSettingsEndpoint
    )

    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Automatic messages configuration"),
      child <-- automaticMessagesStream.splitStatus(
        (resolved, _) =>
          resolved.output match {
            case Right(settings) =>
              val state: Var[AutomaticMessagesSettings] = Var(settings)
              val streamStart: Var[Option[String]] = {
                state.zoomLazy(_.streamStart)((old, newStreamStart) =>
                  old.copy(streamStart = newStreamStart)
                )
              }
              val streamEnd: Var[Option[String]] = {
                state.zoomLazy(_.streamEnd)((old, newStreamEnd) =>
                  old.copy(streamEnd = newStreamEnd)
                )
              }
              val incomingRaid: Var[Option[String]] = {
                state.zoomLazy(_.incomingRaid)((old, newIncomingRaid) =>
                  old.copy(incomingRaid = newIncomingRaid)
                )
              }
              val outgoingRaid: Var[Option[String]] = {
                state.zoomLazy(_.outgoingRaid)((old, newOutgoingRaid) =>
                  old.copy(outgoingRaid = newOutgoingRaid)
                )
              }
              val hypeTrainBegin: Var[Option[String]] = {
                state.zoomLazy(_.hypeTrainBegin)((old, newHypeTrainBegin) =>
                  old.copy(hypeTrainBegin = newHypeTrainBegin)
                )
              }
              val hypeTrainLevelUp: Var[Option[Map[Int, String]]] = {
                state.zoomLazy(_.hypeTrainLevelUp)((old, newHypeTrainLevelUp) =>
                  old.copy(hypeTrainLevelUp = newHypeTrainLevelUp)
                )
              }
              val hypeTrainEnd: Var[Option[String]] = {
                state.zoomLazy(_.hypeTrainEnd)((old, newHypeTrainEnd) =>
                  old.copy(hypeTrainEnd = newHypeTrainEnd)
                )
              }
              val follow: Var[Option[String]] = {
                state.zoomLazy(_.follow)((old, newFollow) =>
                  old.copy(follow = newFollow)
                )
              }
              val subscription: Var[Option[Map[Int, String]]] = {
                state.zoomLazy(_.subscription)((old, newSubscription) =>
                  old.copy(subscription = newSubscription)
                )
              }
              val anonymousSubscriptionGift: Var[Option[Map[Int, String]]] = {
                state.zoomLazy(_.anonymousSubscriptionGift)(
                  (old, newAnonymousSubscriptionGift) =>
                    old.copy(anonymousSubscriptionGift =
                      newAnonymousSubscriptionGift
                    )
                )
              }
              val subscriptionGift: Var[Option[Map[Int, String]]] = {
                state.zoomLazy(_.subscriptionGift)((old, newSubscriptionGift) =>
                  old.copy(subscriptionGift = newSubscriptionGift)
                )
              }
              val cheer: Var[Option[Map[Int, String]]] = {
                state.zoomLazy(_.cheer)((old, newCheer) =>
                  old.copy(cheer = newCheer)
                )
              }
              val anonymousCheer: Var[Option[Map[Int, String]]] = {
                state.zoomLazy(_.anonymousCheer)((old, newAnonymousCheer) =>
                  old.copy(anonymousCheer = newAnonymousCheer)
                )
              }
              val hypeChat: Var[Option[String]] = {
                state.zoomLazy(_.hypeChat)((old, newHypeChat) =>
                  old.copy(hypeChat = newHypeChat)
                )
              }
              val knownGreets: Var[Option[KnownGreetsSettings]] = {
                state.zoomLazy(_.knownGreets)((old, newKnownGreets) =>
                  old.copy(knownGreets = newKnownGreets)
                )
              }
              form(
                onSubmit.preventDefault --> { _ => },
                OptionalInput.render(
                  "Stream start",
                  s"Message sent when the stream starts | The following variables can be used: $${game}, $${title}",
                  streamStart
                ),
                OptionalInput.render(
                  "Stream end",
                  "Message sent when the stream ends",
                  streamEnd
                ),
                OptionalInput.render(
                  "Incoming raid",
                  s"Message sent when there is an incoming raid | The variable $${broadcaster} can be used",
                  incomingRaid
                ),
                OptionalInput.render(
                  "Outgoing raid",
                  s"Message sent when you raid another channel | The variable $${broadcaster} can be used",
                  outgoingRaid
                ),
                OptionalInput.render(
                  "Start of Hype Train",
                  "Message sent when the hype train starts",
                  hypeTrainBegin
                ),
                SaveButton.render[AutomaticMessagesSettings](
                  state,
                  currentSettings =>
                    fetchPostStream(
                      automaticMessagesSettingsEndpoint,
                      currentSettings
                    )
                )
              )

            case Left(ex) =>
              div(s"Failed to load! ${ex.getMessage}")
          },
        (_, _) => div("Loading...")
      )
    )
  }
}
