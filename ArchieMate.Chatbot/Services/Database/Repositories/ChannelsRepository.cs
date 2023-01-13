using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

public class ChannelsRepository : IChannelsRepository
{
    private readonly IDataContext context;
    private readonly ILogger<ChannelsRepository> logger;
    private readonly IWidgetConfigurationsRepository<TTSWidgetConfiguration> ttsWidgetConfigurationsRepository;

    public ChannelsRepository(IDataContext context, IWidgetConfigurationsRepository<TTSWidgetConfiguration> ttsWidgetConfigurationsRepository, ILogger<ChannelsRepository> logger)
    {
        this.ttsWidgetConfigurationsRepository = ttsWidgetConfigurationsRepository;
        this.context = context;
        this.logger = logger;
    }

    public async Task AddAsync(Channel channel)
    {
        await this.context.Channels.AddAsync(channel);
        await this.ttsWidgetConfigurationsRepository.AddAsync(channel.Id);
        await this.context.SaveChangesAsync();
    }

    public async Task DeleteAsync(Channel channel)
    {
        if (await this.GetAsync(channel.Id) is null)
        {
            throw new ArgumentException($"Supplied channel with id {channel.Id} does not exist.", nameof(channel));
        }

        this.context.Channels.Remove(channel);
        await this.context.SaveChangesAsync();
    }

    public async Task<IEnumerable<Channel>> GetAllAsync()
    {
        return await this.context.Channels.AsNoTracking().ToListAsync();
    }

    public async Task<Channel?> GetAsync(Guid id)
    {
        try
        {
            return await this.context.Channels.AsNoTracking().SingleAsync(channel => channel.Id == id);
        }
        catch (InvalidOperationException)
        {
            this.logger.LogWarning($"No channel with guid {id} has been found");
            return null;
        }
    }

    public async Task<Channel?> GetByNameAsync(string name)
    {
        try
        {
            return await this.context.Channels.AsNoTracking().SingleAsync(channel => channel.Name == name);
        }
        catch (System.InvalidOperationException)
        {
            this.logger.LogWarning($"Channel with name {name} not found.");
            return null;
        }
    }

    public async Task<Channel?> GetByRoomIdAsync(int roomId)
    {
        try
        {
            return await this.context.Channels.AsNoTracking().SingleAsync(channel => channel.RoomId == roomId);
        }
        catch (System.InvalidOperationException)
        {
            this.logger.LogWarning($"Channel with roomId {roomId} not found.");
            return null;
        }
    }

    public async Task UpdateAsync(Channel channel)
    {
        if (await this.GetAsync(channel.Id) is null)
        {
            throw new InvalidOperationException($"Supplied channel with id {channel.Id} does not exist.");
        }

        this.context.Channels.Update(channel);
        await this.context.SaveChangesAsync();
    }
}
