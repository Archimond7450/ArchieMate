using System.ComponentModel.DataAnnotations.Schema;

namespace ArchieMate.Chatbot.Models;

public record Command
{
    public Guid Id { get; init; } = new Guid();
    public Guid ChannelId { get; init; } = Guid.NewGuid();
    [ForeignKey(nameof(ChannelId))]
    public Channel Channel { get; init; } = default!;
    public string Name { get; init; } = String.Empty;
    public string Response { get; init; } = String.Empty;
}

public static class CommandExtensions
{
    public static CommandDTO ToDTO(this Command command)
    {
        return new CommandDTO
        {
            Id = command.Id,
            ChannelId = command.ChannelId,
            Name = command.Name,
            Response = command.Response
        };
    }

    public static CommandDTOWithoutChannel ToDTOWithoutChannel(this Command command)
    {
        return new CommandDTOWithoutChannel
        {
            Id = command.Id,
            Name = command.Name,
            Response = command.Response
        };
    }
}
