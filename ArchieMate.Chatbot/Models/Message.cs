using System.ComponentModel.DataAnnotations.Schema;
using ArchieMate.Chatbot.Services.Database.Repositories;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.Chatbot.Models;

public record Message
{
    public required Guid Id { get; init; }
    public required Guid ChannelId { get; init; }
    [ForeignKey(nameof(ChannelId))]
    public Channel Channel { get; init; } = default!;
    public required string DisplayName { get; init; }
    public required string Text { get; init; }
    public required DateTimeOffset Timestamp { get; init; }

    public static async Task<Message?> FromTwitchPrivMsg(PrivMsg msg, IChannelsRepository channelsRepository)
    {
        var channel = await channelsRepository.GetByRoomIdAsync(msg.RoomId);
        if (channel is null)
        {
            return null;
        }

        return new Message
        {
            Id = msg.MessageId,
            ChannelId = channel.Id,
            DisplayName = msg.DisplayName,
            Text = msg.Message,
            Timestamp = msg.Timestamp
        };
    }
}