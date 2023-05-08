using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;

namespace ArchieMate.Chatbot.Services.Database;

public interface IDataContext
{
    DbSet<Channel> Channels { get; set; }
    DbSet<Command> Commands { get; set; }
    DbSet<ChannelVariable> ChannelVariables { get; set; }
    DbSet<TTSWidgetConfiguration> TTSWidgetConfigurations { get; set; }
    DbSet<Message> Messages { get; set; }

    Task<int> SaveChangesAsync(CancellationToken cancellationToken = default);
}