namespace ArchieMate.Chatbot.Models.DTOs.Incoming;

public record CreateCommandDTO
{
    public Guid ChannelId { get; init; } = Guid.NewGuid();
    public string Name { get; init; } = String.Empty;
    public string Response { get; init; } = String.Empty;
}

public static class CreateCommandDTOExtensions
{
    public static Command ToModel(this CreateCommandDTO dto)
    {
        return new Command
        {
            ChannelId = dto.ChannelId,
            Name = dto.Name,
            Response = dto.Response
        };
    }
}
