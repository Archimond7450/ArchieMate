using ArchieMate.Chatbot.Models;
using Microsoft.EntityFrameworkCore;

namespace ArchieMate.Chatbot.Services.Database;

public class PostgreSqlContext : DbContext, IDataContext
{
    public DbSet<Channel> Channels { get; set; } = default!;
    public DbSet<Command> Commands { get; set; } = default!;
    public DbSet<ChannelVariable> ChannelVariables { get; set; } = default!;

    public PostgreSqlContext(DbContextOptions<PostgreSqlContext> options) : base(options)
    {
        AppContext.SetSwitch("Npgsql.EnableLegacyTimestampBehavior", true);
    }
}