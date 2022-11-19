using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

class ChannelVariablesRepository : IChannelVariablesRepository
{
    private readonly IDataContext context;
    private readonly ILogger<CommandsRepository> logger;

    public ChannelVariablesRepository(IDataContext context, ILogger<CommandsRepository> logger)
    {
        this.context = context;
        this.logger = logger;
    }

    public async Task AddAsync(ChannelVariable variable)
    {
        await this.context.ChannelVariables.AddAsync(variable with { Name = variable.Name.ToLowerInvariant() });
        await this.context.SaveChangesAsync();
    }

    public async Task DeleteAsync(ChannelVariable variable)
    {
        if (await this.GetAsync(variable.Id) is null)
        {
            throw new ArgumentException($"Supplied channel variable with id {variable.Id} does not exist.", nameof(variable));
        }

        this.context.ChannelVariables.Remove(variable);
        await this.context.SaveChangesAsync();
    }

    public async Task<IEnumerable<ChannelVariable>> GetAllAsync()
    {
        return await this.context.ChannelVariables.AsNoTracking().ToListAsync();
    }

    public async Task<IEnumerable<ChannelVariable>> GetAllForChannelAsync(string channelName)
    {
        return await this.context.ChannelVariables.Where(command => command.Channel.Name == channelName).AsNoTracking().ToListAsync();
    }

    public async Task<ChannelVariable?> GetAsync(Guid id)
    {
        try
        {
            return await this.context.ChannelVariables.AsNoTracking().SingleAsync(command => command.Id == id);
        }
        catch (InvalidOperationException)
        {
            this.logger.LogWarning($"No channel variable with guid {id} has been found");
            return null;
        }
    }

    public async Task<ChannelVariable?> GetByChannelAndNameAsync(string channelName, string variableName)
    {
        var variables = await this.context.ChannelVariables.Where(variable => variable.Channel.Name == channelName && variable.Name == variableName).AsNoTracking().ToListAsync();
        if (variables.Count != 1)
        {
            if (variables.Count == 0)
            {
                this.logger.LogWarning($"No variable found for channel {channelName} and variable name {variableName}");
            }
            else
            {
                this.logger.LogError($"Too many variables found ({variables.Count}) for channel {channelName} and variable name {variableName}");
            }
            return null;
        }

        return variables.First();
    }

    public async Task UpdateAsync(ChannelVariable variable)
    {
        if (await this.GetAsync(variable.Id) is null)
        {
            throw new InvalidOperationException($"Supplied channel variable with id {variable.Id} does not exist.");
        }

        this.context.ChannelVariables.Update(variable);
        await this.context.SaveChangesAsync();
    }
}