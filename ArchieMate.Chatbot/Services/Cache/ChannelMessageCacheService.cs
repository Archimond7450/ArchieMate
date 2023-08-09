using System;
using System.Globalization;
using ArchieMate.Chatbot.Services.Database.Repositories;
using ArchieMate.TwitchIRC.Messages.Incoming;
using ChatMessage = ArchieMate.Chatbot.Models.Message;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.DependencyInjection;

namespace ArchieMate.Chatbot.Services.Cache
{
    public class ChannelMessageCacheService : IChannelMessageCacheService
    {
        private Dictionary<Guid, List<ChatMessage>> caches = new Dictionary<Guid, List<ChatMessage>>();
        private readonly ILogger<ChannelMessageCacheService> logger;
        private readonly IServiceProvider serviceProvider;

        public ChannelMessageCacheService(ILogger<ChannelMessageCacheService> logger, IServiceProvider serviceProvider)
        {
            this.logger = logger;
            this.serviceProvider = serviceProvider;
            this.logger.LogDebug("Loading last messages from DB");

            this.LoadMessagesFromBackup().Wait();

            this.logger.LogDebug("Initialized ChannelMessageCacheService");
        }

        public async Task AddChannelMessage(Guid channelId, PrivMsg msg)
        {
            this.logger.LogDebug($"ChannelMessageCacheService.AddChannelMessage({channelId}, {msg.Message}");

            using (var scope = this.serviceProvider.CreateScope())
            {
                var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();
                var message = await ChatMessage.FromTwitchPrivMsg(msg, channelsRepository);

                if (message is null)
                {
                    this.logger.LogError("Converted message ready to be cached is null!");
                    return;
                }
                this.logger.LogDebug($"Message ready to be cached: {message}");
                EnsureOneCacheIsCreated(channelId).Add(message);
            }
        }

        public ChatMessage? GetLatestChannelMessage(Guid channelId)
        {
            this.logger.LogDebug($"ChannelMessageCacheService.GetLatestChannelMessage({channelId})");

            var cache = EnsureOneCacheIsCreated(channelId);
            if (!cache.Any())
            {
                this.logger.LogDebug($"No messages yet for channelId: {channelId}");
                return null;
            }
            return EnsureOneCacheIsCreated(channelId).Last();
        }

        public IEnumerable<ChatMessage>? GetChannelMessagesFrom(Guid channelId, Guid messageFromId)
        {
            var cache = EnsureOneCacheIsCreated(channelId);
            var msgFrom = cache.Find(msg => msg.Id == messageFromId);
            if (msgFrom is null)
            {
                return null;
            }

            return cache.FindAll(msg => DateTimeOffset.Compare(msgFrom.Timestamp, msg.Timestamp) < 0);
        }

        public async Task CleanCacheAndBackup()
        {
            foreach (Guid channelId in caches.Keys)
            {
                var oneCache = caches[channelId];
                var lastMessage = oneCache.Last();

                oneCache.RemoveAll(IsOldMessage);

                if (oneCache.Count == 0)
                {
                    oneCache.Add(lastMessage);
                }

                await this.BackupOneMessage(lastMessage);
            }
        }

        private async Task LoadMessagesFromBackup()
        {
            using (var scope = this.serviceProvider.CreateScope())
            {
                var messagesRepository = scope.ServiceProvider.GetRequiredService<IMessagesRepository>();
                foreach (var message in await messagesRepository.GetAllAsync())
                {
                    var cache = EnsureOneCacheIsCreated(message.ChannelId);
                    cache.Add(message);
                }
            }
        }

        private async Task BackupOneMessage(ChatMessage? message)
        {
            if (message is null)
            {
                return;
            }

            using (var scope = this.serviceProvider.CreateScope())
            {
                var messagesRepository = scope.ServiceProvider.GetRequiredService<IMessagesRepository>();
                await messagesRepository.AddAsync(message);
            }
        }

        private List<ChatMessage> EnsureOneCacheIsCreated(Guid channelId)
        {
            if (!caches.ContainsKey(channelId))
            {
                caches.Add(channelId, new List<ChatMessage>());
            }

            return caches[channelId];
        }

        private static bool IsOldMessage(ChatMessage msg)
        {
            var difference = DateTimeOffset.Now.Subtract(msg.Timestamp);
            return difference.TotalMinutes >= 1;
        }

        public Dictionary<Guid, List<ChatMessage>> GetAll()
        {
            return caches;
        }
    }
}