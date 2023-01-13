using System.ComponentModel.DataAnnotations;

namespace ArchieMate.Chatbot.Models.DTOs.Outgoing;
public record TTSWidgetConfigurationDTO
{
    public bool Enabled { get; init; } = false;
    public bool OnlyFromMentions { get; init; } = true;
    public uint DelayBetweenMessages { get; init; } = 10;
    [Range(0, 100, ErrorMessage = "VolumePercent must be between 0 and 100.")]
    public uint VolumePercent { get; init; } = 75;
    public uint MaxDuration { get; init; } = 10;
    public bool AllowEmoteOnly { get; init; } = false;
    public bool Censoring { get; init; } = true;
}
