package com.archimond7450.archiemate.actors.repositories

import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.apache.pekko.serialization.Serializer

import scala.util.Try

abstract class GenericSerializer[Event <: AnyRef](
    repositoryName: String,
    id: Int
)(using
    Decoder[Event],
    Encoder[Event]
) extends Serializer {
  val toEvent: PartialFunction[AnyRef, Event]

  private val serializeEvent: Event => String = _.asJson.noSpaces
  private val deserializeEvent: String => Event = str =>
    decode[Event](str) match {
      case Right(event) => event
      case Left(ex)     => throw ex
    }

  override def identifier: Int = id

  override def toBinary(o: AnyRef): Array[Byte] = {
    if (toEvent.isDefinedAt(o)) {
      serializeEvent(toEvent(o)).getBytes
    } else {
      throw new IllegalArgumentException(
        s"This serializer supports only $repositoryName events."
      )
    }
  }

  override def includeManifest: Boolean = false

  override def fromBinary(
      bytes: Array[Byte],
      manifest: Option[Class[_]]
  ): AnyRef = {
    val string = new String(bytes)
    deserializeEvent(string)
  }
}
