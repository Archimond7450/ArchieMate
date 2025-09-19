package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.matching.Regex

class TagsDecoder {
  private val log = LoggerFactory.getLogger(getClass)

  private val tagsRegex: Regex = """@(([^=;\s]+=[^=;\s]*)(;[^=;\s]+=[^=;\s]*)*)""".r

  def decode(tags: String): Option[Map[String, String]] = tags match {
    case tagsRegex(allTags, _, _) =>
      Some(
        allTags
          .split(";")
          .map(_.split("="))
          .map(a => a(0) -> (if (a.length > 1) unescapeValue(a(1)) else ""))
          .toMap
      )
    case _ =>
      log.error("Cannot decode invalid tags: {}", tags)
      None
  }

  private[irc] def unescapeValue(value: String): String = {
    @tailrec
    def unescape(remaining: String, accumulator: String = "", wasBackSlash: Boolean = false): String = {
      if (remaining == "") {
        accumulator
      } else {
        val isBackSlash = remaining(0) == '\\'
        val next =
          if (wasBackSlash) {
            remaining(0) match {
              case ':' => ";"
              case 's' => " "
              case '\\' => "\\"
              case 'r' => "\r"
              case 'n' => "\n"
              case other => other.toString
            }
          } else {
            if (isBackSlash) ""
            else remaining(0)
          }
        unescape(remaining.substring(1), s"$accumulator$next", !wasBackSlash && isBackSlash)
      }
    }

    unescape(value)
  }
}
