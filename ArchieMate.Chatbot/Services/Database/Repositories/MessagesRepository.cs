using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services.Database.Repositories;

class MessagesRepository : IMessagesRepository
{
    private readonly IDataContext context;
    private readonly ILogger<MessagesRepository> logger;

    public MessagesRepository(IDataContext context, ILogger<MessagesRepository> logger)
    {
        this.context = context;
        this.logger = logger;
    }

    public async Task AddAsync(Message message)
    {
        if (await this.context.Messages.FindAsync(message.ChannelId) is Message existingMessage)
        {
            this.context.Messages.Update(existingMessage);
        }
        else
        {
            await this.context.Messages.AddAsync(message);
        }
        await this.context.SaveChangesAsync();
    }

    public async Task<IEnumerable<Message>> GetAllAsync()
    {
        return await this.context.Messages.AsNoTracking().ToListAsync();
    }
}