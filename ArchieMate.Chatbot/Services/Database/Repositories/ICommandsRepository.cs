using ArchieMate.Chatbot.Models;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

public interface ICommandsRepository
{
    Task<Command?> GetAsync(Guid id);
    Task<Command?> GetByChannelAndNameAsync(string channelName, string commandName);
    Task<IEnumerable<Command>> GetAllAsync();
    Task<IEnumerable<Command>> GetAllForChannelAsync(string channelName);
    Task AddAsync(Command command);
    Task UpdateAsync(Command command);
    Task DeleteAsync(Command command);
}