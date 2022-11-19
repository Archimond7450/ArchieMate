using ArchieMate.Chatbot.Models;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

public interface IChannelVariablesRepository
{
    Task<ChannelVariable?> GetAsync(Guid id);
    Task<ChannelVariable?> GetByChannelAndNameAsync(string channelName, string commandName);
    Task<IEnumerable<ChannelVariable>> GetAllAsync();
    Task<IEnumerable<ChannelVariable>> GetAllForChannelAsync(string channelName);
    Task AddAsync(ChannelVariable command);
    Task UpdateAsync(ChannelVariable command);
    Task DeleteAsync(ChannelVariable command);
}