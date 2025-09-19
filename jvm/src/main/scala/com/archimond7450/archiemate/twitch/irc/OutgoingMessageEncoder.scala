package com.archimond7450.archiemate.twitch.irc

import com.archimond7450.archiemate.twitch.irc.OutgoingMessages._

class OutgoingMessageEncoder {
  def encode(message: OutgoingMessage): String = message match {
    case Pass(token) => s"PASS oauth:$token"
    case Nick(username) => s"NICK $username"
    case CapabilityRequest(capabilities) => s"CAP REQ :${capabilities.mkString(" ")}"
    case Join(channelNames) => s"JOIN ${channelNames.map(channelName => s"#${channelName}").mkString(",")}"
    case Pong => "PONG :tmi.twitch.tv"
    case PrivMsg(channel, message, None) =>
      message.split("\n").map(line => s"PRIVMSG #$channel :${line.trim}").mkString("\n")
    case PrivMsg(channel, message, Some(replyToMessageId)) =>
      message.split("\n").map(line => s"@reply-parent-msg-id=$replyToMessageId PRIVMSG #$channel :${line.trim}").mkString("\n")
    case Part(channelName) => s"PART #$channelName"
  }
}