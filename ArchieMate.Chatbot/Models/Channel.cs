using ArchieMate.Chatbot.Models.DTOs.Outgoing;

namespace ArchieMate.Chatbot.Models;

public record Channel
{
    public const int InvalidRoomId = 0;

    public Guid Id { get; init; } = Guid.NewGuid();
    public string Name { get; init; } = String.Empty;
    public int RoomId { get; init; } = InvalidRoomId;
    public bool Join { get; init; } = false;
}

public static class ChannelExtensions
{
    public static ChannelDTO ToDTO(this Channel channel)
    {
        return new ChannelDTO
        {
            Id = channel.Id,
            Name = channel.Name,
            RoomId = channel.RoomId,
            Join = channel.Join
        };
    }
}