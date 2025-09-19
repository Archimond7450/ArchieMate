package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

class BadgesDecoder {
  private val log = LoggerFactory.getLogger(getClass)

  private val badgesRegex = """((\S+/\S+)(,\S+/\S+)*)""".r

  def decode(badges: String): Map[String, String] = badges match {
    case "" => Map.empty
    case badgesRegex(allBadges, _, _) =>
      allBadges
        .split(",")
        .map(_.split("/"))
        .map(b => (b(0), b(1)))
        .toMap
    case _ =>
      log.error("Cannot decode invalid badges tag: {}", badges)
      Map.empty
  }
}
