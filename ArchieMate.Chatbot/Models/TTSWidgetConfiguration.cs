using System.ComponentModel.DataAnnotations;
using ArchieMate.Chatbot.Models.DTOs.Outgoing;

namespace ArchieMate.Chatbot.Models;
public record TTSWidgetConfiguration
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public required Guid ChannelId { get; init; } // TODO: Maybe use ForeignKey decorator here??? Or just depend on the fact that there will be no deleting here???
    public bool Enabled { get; init; } = false;
    public bool OnlyFromMentions { get; init; } = true;
    public uint DelayBetweenMessages { get; init; } = 10;
    [Range(0, 100, ErrorMessage = "VolumePercent must be between 0 and 100.")]
    public uint VolumePercent { get; init; } = 75;
    public uint MaxDuration { get; init; } = 10;
    public bool AllowEmoteOnly { get; init; } = false;
    public bool Censoring { get; init; } = true;
}

public static class TTSWidgetConfigurationExtensions
{
    public static TTSWidgetConfigurationDTO ToDTO(this TTSWidgetConfiguration config)
    {
        return new TTSWidgetConfigurationDTO
        {
            Enabled = config.Enabled,
            OnlyFromMentions = config.OnlyFromMentions,
            DelayBetweenMessages = config.DelayBetweenMessages,
            VolumePercent = config.VolumePercent,
            MaxDuration = config.MaxDuration,
            AllowEmoteOnly = config.AllowEmoteOnly,
            Censoring = config.Censoring
        };
    }
}
