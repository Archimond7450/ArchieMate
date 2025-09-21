package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object ChannelSettings {
  final case class Settings(
      basicChatbotSettings: BasicChatbotSettings = BasicChatbotSettings(),
      builtInCommandsSettings: BuiltInCommandsSettings =
        BuiltInCommandsSettings(),
      commandsSettings: CommandsSettings = CommandsSettings(),
      variablesSettings: VariablesSettings = VariablesSettings(),
      timersSettings: TimersSettings = TimersSettings(),
      overlaysSettings: OverlaysSettings = OverlaysSettings(),
      automaticMessagesSettings: AutomaticMessagesSettings =
        AutomaticMessagesSettings()
  )

  object Settings {
    given Decoder[Settings] = ConfiguredDecoder.derived
    given Encoder[Settings] = ConfiguredEncoder.derived
  }

  final case class BasicChatbotSettings(join: Boolean = false)

  object BasicChatbotSettings {
    given Decoder[BasicChatbotSettings] = ConfiguredDecoder.derived
    given Encoder[BasicChatbotSettings] = ConfiguredEncoder.derived
  }

  final case class BuiltInCommandsSettings(
      game: Boolean = false,
      title: Boolean = false,
      subs: Boolean = false,
      uptime: Boolean = false,
      followage: Boolean = false,
      afk: Boolean = false
  )

  case object BuiltInCommandsSettings {
    given Decoder[BuiltInCommandsSettings] = ConfiguredDecoder.derived
    given Encoder[BuiltInCommandsSettings] = ConfiguredEncoder.derived
  }

  final case class CommandsSettings(
      commands: List[ChannelCommand] = Nil
  )

  object CommandsSettings {
    given Decoder[CommandsSettings] = ConfiguredDecoder.derived
    given Encoder[CommandsSettings] = ConfiguredEncoder.derived
  }

  final case class VariablesSettings(
      variables: List[ChannelVariable] = Nil
  )

  object VariablesSettings {
    given Decoder[VariablesSettings] = ConfiguredDecoder.derived
    given Encoder[VariablesSettings] = ConfiguredEncoder.derived
  }

  final case class TimersSettings(
      enabled: Boolean = true,
      intervalMinutes: Int = 5,
      minimumMessages: Int = 5,
      youTubeVideosOccurrence: YouTubeVideosOccurrence =
        YouTubeVideosOccurrence.Never,
      timersUntilNextAllowedRepeat: Int = 1,
      commands: Set[String] = Set.empty,
      manualTimers: List[ManualTimer] = Nil
  )

  object TimersSettings {
    given Decoder[TimersSettings] = ConfiguredDecoder.derived
    given Encoder[TimersSettings] = ConfiguredEncoder.derived
  }

  final case class ManualTimer(
      id: String,
      response: String
  )

  object ManualTimer {
    given Decoder[ManualTimer] = ConfiguredDecoder.derived
    given Encoder[ManualTimer] = ConfiguredEncoder.derived
  }

  enum YouTubeVideosOccurrence {
    case Never
    case OneInTen
    case OneInNine
    case OneInEight
    case OneInSeven
    case OneInSix
    case OneInFive
    case OneInFour
    case OneInThree
    case OneInTwo
  }

  object YouTubeVideosOccurrence {
    given Decoder[YouTubeVideosOccurrence] = ConfiguredDecoder.derived
    given Encoder[YouTubeVideosOccurrence] = ConfiguredEncoder.derived
  }

  final case class ChannelCommand(
      id: Option[String],
      name: String,
      response: String
  )

  object ChannelCommand {
    given Decoder[ChannelCommand] = ConfiguredDecoder.derived
    given Encoder[ChannelCommand] = ConfiguredEncoder.derived
  }

  final case class ChannelVariable(
      id: Option[String],
      name: String,
      value: String
  )

  object ChannelVariable {
    given Decoder[ChannelVariable] = ConfiguredDecoder.derived
    given Encoder[ChannelVariable] = ConfiguredEncoder.derived
  }

  final case class OverlaysSettings(
      currentOverlayId: String = "",
      overlaysSettings: Map[String, OverlaySettings] = Map.empty
  )

  object OverlaysSettings {
    given Decoder[OverlaysSettings] = ConfiguredDecoder.derived
    given Encoder[OverlaysSettings] = ConfiguredEncoder.derived
  }

  final case class OverlaySettings(
      ttsWidgetSettings: Option[TTSWidgetSettings] = None
  )

  object OverlaySettings {
    given Decoder[OverlaySettings] = ConfiguredDecoder.derived
    given Encoder[OverlaySettings] = ConfiguredEncoder.derived
  }

  final case class TTSWidgetSettings(
      x: Int = 1400,
      y: Int = 300,
      width: Int = 400,
      height: Int = 200,
      mode: TTSWidgetMode = TTSWidgetMode.ModsVipsSubs,
      delayBetweenNotificationsSeconds: Int = 10,
      maxNotificationLengthSeconds: Int = 50,
      ignoreWords: Set[String] = Set.empty,
      allowOnlyAsciiCharacters: Boolean = false,
      userNameColor: String = "#32C3A6",
      blockLinks: Boolean = true,
      replacements: Map[String, String] = Map.empty,
      useGreetName: Boolean = true
  )

  object TTSWidgetSettings {
    given Decoder[TTSWidgetSettings] = ConfiguredDecoder.derived
    given Encoder[TTSWidgetSettings] = ConfiguredEncoder.derived
  }

  enum TTSWidgetMode {
    case All
    case Mods
    case ModsVips
    case ModsVipsSubs
    case ChannelPointReward
  }

  object TTSWidgetMode {
    given Decoder[TTSWidgetMode] = ConfiguredDecoder.derived
    given Encoder[TTSWidgetMode] = ConfiguredEncoder.derived
  }

  final case class AutomaticMessagesSettings(
      streamStart: Option[String] = None,
      streamEnd: Option[String] = None,
      incomingRaid: Option[String] = None,
      outgoingRaid: Option[String] = None,
      hypeTrainBegin: Option[String] = None,
      hypeTrainLevelUp: Option[Map[Int, String]] = None,
      hypeTrainEnd: Option[String] = None,
      follow: Option[String] = None,
      subscription: Option[Map[Int, String]] = None,
      anonymousSubscriptionGift: Option[Map[Int, String]] = None,
      subscriptionGift: Option[Map[Int, String]] = None,
      cheer: Option[Map[Int, String]] = None,
      anonymousCheer: Option[Map[Int, String]] = None,
      hypeChat: Option[String] = None,
      knownGreets: Option[KnownGreetsSettings] = None
  )

  object AutomaticMessagesSettings {
    given Decoder[AutomaticMessagesSettings] = ConfiguredDecoder.derived
    given Encoder[AutomaticMessagesSettings] = ConfiguredEncoder.derived
  }

  final case class KnownGreetsSettings(
      mode: KnownGreetsMode = KnownGreetsMode.ModsVipsSubs,
      standardGreets: List[String] = List("Hi ${user}"),
      specificNames: Map[String, String] = Map.empty,
      specificGreets: Map[String, String] = Map.empty
  )

  object KnownGreetsSettings {
    given Decoder[KnownGreetsSettings] = ConfiguredDecoder.derived
    given Encoder[KnownGreetsSettings] = ConfiguredEncoder.derived
  }

  enum KnownGreetsMode {
    case All
    case Mods
    case ModsVips
    case ModsVipsSubs
  }

  object KnownGreetsMode {
    given Decoder[KnownGreetsMode] = ConfiguredDecoder.derived
    given Encoder[KnownGreetsMode] = ConfiguredEncoder.derived
  }
}
