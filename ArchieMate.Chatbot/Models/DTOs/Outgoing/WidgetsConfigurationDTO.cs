namespace ArchieMate.Chatbot.Models.DTOs.Outgoing;

public record WidgetsConfigurationDTO
{
    public required TTSWidgetConfigurationDTO TextToSpeech { get; init; }
}
