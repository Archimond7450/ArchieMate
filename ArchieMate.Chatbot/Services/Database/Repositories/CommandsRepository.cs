using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

public class CommandsRepository : ICommandsRepository
{
    private readonly IDataContext context;
    private readonly ILogger<CommandsRepository> logger;

    public CommandsRepository(IDataContext context, ILogger<CommandsRepository> logger)
    {
        this.context = context;
        this.logger = logger;
    }

    public async Task AddAsync(Command command)
    {
        await this.context.Commands.AddAsync(command with { Name = command.Name.ToLowerInvariant() });
        await this.context.SaveChangesAsync();
    }

    public async Task DeleteAsync(Command command)
    {
        if (await this.GetAsync(command.Id) is null)
        {
            throw new ArgumentException($"Supplied command with id {command.Id} does not exist.", nameof(command));
        }

        this.context.Commands.Remove(command);
        await this.context.SaveChangesAsync();
    }

    public async Task<IEnumerable<Command>> GetAllAsync()
    {
        return await this.context.Commands.AsNoTracking().ToListAsync();
    }

    public async Task<IEnumerable<Command>> GetAllForChannelAsync(string channelName)
    {
        return await this.context.Commands.Where(command => command.Channel.Name == channelName).AsNoTracking().ToListAsync();
    }

    public async Task<Command?> GetAsync(Guid id)
    {
        try
        {
            return await this.context.Commands.AsNoTracking().SingleAsync(command => command.Id == id);
        }
        catch (InvalidOperationException)
        {
            this.logger.LogWarning($"No command with guid {id} has been found");
            return null;
        }
    }

    public async Task<Command?> GetByChannelAndNameAsync(string channelName, string commandName)
    {
        var commands = await this.context.Commands.Where(command => command.Channel.Name == channelName && command.Name == commandName).AsNoTracking().ToListAsync();
        if (commands.Count != 1)
        {
            if (commands.Count == 0)
            {
                this.logger.LogWarning($"No commands found for channel {channelName} and command name {commandName}");
            }
            else
            {
                this.logger.LogError($"Too many commands found ({commands.Count}) for channel {channelName} and command name {commandName}");
            }
            return null;
        }

        return commands.First();
    }

    public async Task UpdateAsync(Command command)
    {
        if (await this.GetAsync(command.Id) is null)
        {
            throw new InvalidOperationException($"Supplied command with id {command.Id} does not exist.");
        }

        this.context.Commands.Update(command);
        await this.context.SaveChangesAsync();
    }
}