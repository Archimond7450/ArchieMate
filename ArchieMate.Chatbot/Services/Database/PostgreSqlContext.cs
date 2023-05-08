using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;

namespace ArchieMate.Chatbot.Services.Database;

public class PostgreSqlContext : DbContext, IDataContext
{
    public required DbSet<Channel> Channels { get; set; }
    public required DbSet<Command> Commands { get; set; }
    public required DbSet<ChannelVariable> ChannelVariables { get; set; }
    public required DbSet<TTSWidgetConfiguration> TTSWidgetConfigurations { get; set; }
    public required DbSet<Message> Messages { get; set; }

    public PostgreSqlContext(DbContextOptions<PostgreSqlContext> options) : base(options)
    {
        AppContext.SetSwitch("Npgsql.EnableLegacyTimestampBehavior", true);
    }
}