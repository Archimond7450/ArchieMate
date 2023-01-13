using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

public class TTSWidgetConfigurationsRepository : IWidgetConfigurationsRepository<TTSWidgetConfiguration>
{
    private readonly IDataContext context;
    private ILogger<TTSWidgetConfigurationsRepository> logger;

    public TTSWidgetConfigurationsRepository(IDataContext context, ILogger<TTSWidgetConfigurationsRepository> logger)
    {
        this.logger = logger;
        this.context = context;
    }

    public async Task AddAsync(Guid channelId)
    {
        if (await this.GetByChannelIdAsync(channelId) is TTSWidgetConfiguration)
        {
            throw new InvalidOperationException($"TTS Widget Configuration for channel with id {channelId} already exists.");
        }

        await this.context.TTSWidgetConfigurations.AddAsync(new TTSWidgetConfiguration
        {
            ChannelId = channelId
        });
        await this.context.SaveChangesAsync();
    }

    public async Task<TTSWidgetConfiguration?> GetByChannelIdAsync(Guid channelId)
    {
        return await this.context.TTSWidgetConfigurations.Where(c => c.ChannelId == channelId).AsNoTracking().FirstOrDefaultAsync();
    }

    public async Task ResetAsync(Guid channelId)
    {
        var foundConfig = await this.GetByChannelIdAsync(channelId);
        if (foundConfig is null)
        {
            throw new InvalidOperationException($"Supplied TTS Widget configuration with channelId {channelId} does not exist.");
        }

        TTSWidgetConfiguration defaultConfiguration = new TTSWidgetConfiguration { Id = foundConfig.Id, ChannelId = foundConfig.ChannelId };

        this.context.TTSWidgetConfigurations.Update(defaultConfiguration);
        await this.context.SaveChangesAsync();
    }

    public async Task UpdateAsync(TTSWidgetConfiguration ttsWidgetConfiguration)
    {
        var foundConfig = await this.context.TTSWidgetConfigurations.Where(conf => conf.Id == ttsWidgetConfiguration.Id).AsNoTracking().FirstAsync();
        if (foundConfig is null)
        {
            throw new InvalidOperationException($"Supplied TTS Widget configuration with id {ttsWidgetConfiguration.Id} does not exist.");
        }

        this.context.TTSWidgetConfigurations.Update(ttsWidgetConfiguration);

        await this.context.SaveChangesAsync();
    }
}
