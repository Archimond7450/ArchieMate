using System.ComponentModel.DataAnnotations;

namespace ArchieMate.Chatbot.Models.DTOs.Incoming;
public record UpdateTTSWidgetConfigurationDTO
{
    public required bool Enabled { get; init; }
    public required bool OnlyFromMentions { get; init; }
    public required uint DelayBetweenMessages { get; init; }
    [Range(0, 100, ErrorMessage = "VolumePercent must be between 0 and 100.")]
    public required uint VolumePercent { get; init; }
    public required uint MaxDuration { get; init; }
    public required bool AllowEmoteOnly { get; init; }
    public required bool Censoring { get; init; }
}

public static class UpdateTTSWidgetConfigurationDTOExtensions
{
    public static TTSWidgetConfiguration ToModel(this UpdateTTSWidgetConfigurationDTO dto, Guid id, Guid channelId)
    {
        return new TTSWidgetConfiguration
        {
            Id = id,
            ChannelId = channelId,
            Enabled = dto.Enabled,
            OnlyFromMentions = dto.OnlyFromMentions,
            DelayBetweenMessages = dto.DelayBetweenMessages,
            VolumePercent = dto.VolumePercent,
            MaxDuration = dto.MaxDuration,
            AllowEmoteOnly = dto.AllowEmoteOnly,
            Censoring = dto.Censoring,
        };
    }
}
