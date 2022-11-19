using System.ComponentModel.DataAnnotations.Schema;

namespace ArchieMate.Chatbot.Models;

public record Message
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public Guid ChannelId { get; init; } = Guid.NewGuid();
    [ForeignKey(nameof(ChannelId))]
    public Channel Channel { get; init; } = default!;

}