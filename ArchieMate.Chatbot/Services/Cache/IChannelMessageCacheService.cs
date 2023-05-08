using ArchieMate.TwitchIRC.Messages.Incoming;
using ChatMessage = ArchieMate.Chatbot.Models.Message;

namespace ArchieMate.Chatbot.Services.Cache
{
    public interface IChannelMessageCacheService
    {
        public Task AddChannelMessage(Guid channelId, PrivMsg msg);
        public ChatMessage GetLatestChannelMessage(Guid channelId);
        public IEnumerable<ChatMessage>? GetChannelMessagesFrom(Guid channelId, Guid messageFromId);
        public Dictionary<Guid, List<ChatMessage>> GetAll();

        public Task CleanCacheAndBackup();
    }
}