package com.archimond7450.archiemate.twitch.irc

trait OutgoingMessage

object OutgoingMessages {
  case class Pass(token: String) extends OutgoingMessage
  case class Nick(username: String) extends OutgoingMessage
  case class CapabilityRequest(capabilities: List[String]) extends OutgoingMessage
  case class Join(channelNames: List[String]) extends OutgoingMessage
  case object Pong extends OutgoingMessage
  case class PrivMsg(channel: String, message: String, replyToMessageId: Option[String] = None) extends OutgoingMessage
  case class Part(channelName: String) extends OutgoingMessage
}