namespace ArchieMate.Chatbot.Models.DTOs.Incoming;

public record UpdateChannelDTO
{
    public string Name { get; init; } = String.Empty;
    public int RoomId { get; init; } = 0;
    public bool Join { get; init; } = false;
}
