using System.ComponentModel.DataAnnotations.Schema;

namespace ArchieMate.Chatbot.Models;

public record ChannelVariable
{
    public Guid Id { get; init; } = new Guid();
    public Guid ChannelId { get; init; } = Guid.NewGuid();
    [ForeignKey(nameof(ChannelId))]
    public Channel Channel { get; init; } = default!;
    public string Name { get; init; } = String.Empty;
    public string Value { get; init; } = String.Empty;
}
