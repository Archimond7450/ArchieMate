using ArchieMate.Chatbot.Models;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

public interface IChannelsRepository
{
    Task<Channel?> GetAsync(Guid id);
    Task<Channel?> GetByRoomIdAsync(int roomId);
    Task<Channel?> GetByNameAsync(string name);
    Task<IEnumerable<Channel>> GetAllAsync();
    Task AddAsync(Channel channel);
    Task UpdateAsync(Channel channel);
    Task DeleteAsync(Channel channel);
}