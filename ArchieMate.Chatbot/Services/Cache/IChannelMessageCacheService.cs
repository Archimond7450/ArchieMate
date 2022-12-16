using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.Chatbot.Services.Cache
{
    public interface IChannelMessageCacheService
    {
        public void AddChannelMessage(Guid channelId, PrivMsg msg);
        public PrivMsg? GetLatestChannelMessage(Guid channelId);
        public IEnumerable<PrivMsg>? GetChannelMessagesFrom(Guid channelId, Guid messageFromId);
        public Dictionary<Guid, List<PrivMsg>> GetAll();

        public void CleanCache();
    }
}