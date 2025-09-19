package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

enum TwitchUserType(val raw: String) {
  case Administrator extends TwitchUserType("admin")
  case Moderator extends TwitchUserType("mod")
  case GlobalModerator extends TwitchUserType("global_mod")
  case TwitchEmployee extends TwitchUserType("staff")
  case NormalUser extends TwitchUserType("")
}

object TwitchUserType {
  def apply(raw: String): Option[TwitchUserType] = TwitchUserType.values.find(x => x.raw == raw)
}
