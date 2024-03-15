namespace ArchieMate.Chatbot.Models.DTOs.Outgoing;

public record MessageDTO
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public Guid ChannelId { get; init; } = Guid.NewGuid();

}