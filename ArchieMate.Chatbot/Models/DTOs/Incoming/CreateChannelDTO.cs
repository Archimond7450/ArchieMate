namespace ArchieMate.Chatbot.Models.DTOs.Incoming;

public record CreateChannelDTO
{
    public string Name { get; init; } = String.Empty;
    public int RoomId { get; init; } = 0;
    public bool Join { get; set; } = true;
}

public static class CreateChannelDTOExtensions
{
    public static Channel ToModel(this CreateChannelDTO dto)
    {
        return new Channel
        {
            Name = dto.Name,
            RoomId = dto.RoomId,
            Join = dto.Join
        };
    }
}