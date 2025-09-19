package com.archimond7450.archiemate.helpers

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.jawn.decode

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

object JsonHelper {
  def decodeOrThrow[A](json: String)(using Decoder[A]): A = {
    decode[A](json) match {
      case Right(value) => value
      case Left(error) => throw new RuntimeException("Decoding failed", error)
    }
  }

  def decodeToTry[A](json: String)(using Decoder[A]): Try[A] = {
    Try(decodeOrThrow(json))
  }

  def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.deepDropNullValues)

  object OffsetDateTimeJson {
    given offsetDateTimeDecoder: Decoder[OffsetDateTime] = (c: HCursor) => {
      c.as[String].map(s => OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    given offsetDateTimeEncoder: Encoder[OffsetDateTime] = (o: OffsetDateTime) => {
      Json.fromString(o.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
  }
}
