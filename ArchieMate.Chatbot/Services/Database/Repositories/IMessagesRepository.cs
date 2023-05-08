
using ArchieMate.Chatbot.Models;

namespace ArchieMate.Chatbot.Services.Database.Repositories
{
    public interface IMessagesRepository
    {
        public Task AddAsync(Message message);
        public Task<IEnumerable<Message>> GetAllAsync();
    }
}