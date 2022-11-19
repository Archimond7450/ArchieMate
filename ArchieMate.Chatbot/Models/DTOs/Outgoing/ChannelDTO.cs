namespace ArchieMate.Chatbot.Models.DTOs.Outgoing;

public record ChannelDTO
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public string Name { get; init; } = String.Empty;
    public int RoomId { get; init; } = 0;
    public bool Join { get; init; } = false;
}