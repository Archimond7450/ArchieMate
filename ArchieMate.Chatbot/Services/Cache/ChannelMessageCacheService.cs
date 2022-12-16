using ArchieMate.TwitchIRC.Messages.Incoming;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services.Cache
{
    public class ChannelMessageCacheService : IChannelMessageCacheService
    {
        private Dictionary<Guid, List<PrivMsg>> caches = new Dictionary<Guid, List<PrivMsg>>();
        private readonly ILogger<ChannelMessageCacheService> logger;

        public ChannelMessageCacheService(ILogger<ChannelMessageCacheService> logger)
        {
            this.logger = logger;
            this.logger.LogDebug("Initialized ChannelMessageCacheService");
        }

        public void AddChannelMessage(Guid channelId, PrivMsg msg)
        {
            this.logger.LogDebug($"ChannelMessageCacheService.AddChannelMessage(${channelId}, ${msg.Message}");
            EnsureOneCacheIsCreated(channelId).Add(msg);
        }

        public PrivMsg? GetLatestChannelMessage(Guid channelId)
        {
            return EnsureOneCacheIsCreated(channelId).LastOrDefault();
        }

        public IEnumerable<PrivMsg>? GetChannelMessagesFrom(Guid channelId, Guid messageFromId)
        {
            var cache = EnsureOneCacheIsCreated(channelId);
            var msgFrom = cache.Find(msg => msg.MessageId == messageFromId);
            if (msgFrom is null)
            {
                return null;
            }

            return cache.FindAll(msg => DateTimeOffset.Compare(msgFrom.Timestamp, msg.Timestamp) < 0);
        }

        public void CleanCache()
        {
            foreach (Guid channelId in caches.Keys)
            {
                var oneCache = caches[channelId];
                var lastMessage = oneCache.LastOrDefault();

                oneCache.RemoveAll(IsOldMessage);

                if (lastMessage is PrivMsg && oneCache.Count == 0)
                {
                    oneCache.Add(lastMessage);
                }
            }
        }

        private List<PrivMsg> EnsureOneCacheIsCreated(Guid channelId)
        {
            if (!caches.ContainsKey(channelId))
            {
                caches.Add(channelId, new List<PrivMsg>());
            }

            return caches[channelId];
        }

        private static bool IsOldMessage(PrivMsg msg)
        {
            var difference = DateTimeOffset.Now.Subtract(msg.Timestamp);
            return difference.TotalMinutes >= 1;
        }

        public Dictionary<Guid, List<PrivMsg>> GetAll()
        {
            return caches;
        }
    }
}