package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

class BadgeInfoDecoder {
  private val log = LoggerFactory.getLogger(getClass)

  private val badgeInfoRegex = """((\S+/.+)(,\S+/.+)*)""".r

  def decode(badgeInfo: String): Map[String, String] = badgeInfo match {
    case "" => Map.empty
    case badgeInfoRegex(allBadgeInfos, _, _) =>
      allBadgeInfos
        .split(",")
        .map(_.split("/"))
        .map(b => (b(0), b(1)))
        .toMap
    case _ =>
      log.error("Cannot decode badge info tag: {} - returning empty map", badgeInfo)
      Map.empty
  }
}
