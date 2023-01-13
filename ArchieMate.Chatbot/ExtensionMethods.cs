using System.Text;
using ArchieMate.Chatbot.Options;
using ArchieMate.Chatbot.Services;
using ArchieMate.Chatbot.Services.Database;
using ArchieMate.Chatbot.Services.Database.Repositories;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using System.Text.Json;
using System.Net.Mime;
using Microsoft.AspNetCore.Builder;
using ArchieMate.Chatbot.Services.Cache;
using ArchieMate.Chatbot.Models;

namespace ArchieMate.Chatbot;

public static class ExtensionMethods
{
    const string DB_READY = "db_ready";

    public static IServiceCollection UseChatbotServices(this IServiceCollection services, ConfigurationManager configuration)
    {
        services.Configure<AuthOptions>(configuration.GetSection(nameof(AuthOptions)));

        services.AddSingleton<ChatbotService>();
        services.AddHostedService<ChatbotService>(provider => provider.GetRequiredService<ChatbotService>());

        // Construct the connection string to the DB
        var postgresDb = Environment.GetEnvironmentVariable(Constants.EnvironmentVariables.POSTGRES_DB);
        var postgresUser = Environment.GetEnvironmentVariable(Constants.EnvironmentVariables.POSTGRES_USER);
        var postgresPassword = Environment.GetEnvironmentVariable(Constants.EnvironmentVariables.POSTGRES_PASSWORD);
        var postgresHost = Environment.GetEnvironmentVariable(Constants.EnvironmentVariables.POSTGRES_HOST) ?? Constants.DefaultPostgreSqlHost;

        var connectionString = $"Server={postgresHost};Port={Constants.PostgreSqlPort};Database={postgresDb};User Id={postgresUser};Password={postgresPassword};";

        // Set up the DB Context, add it as singleton and add each repositories
        services.AddDbContext<IDataContext, PostgreSqlContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IDataContext, PostgreSqlContext>();
        services.AddScoped<IChannelsRepository, ChannelsRepository>();
        services.AddScoped<ICommandsRepository, CommandsRepository>();
        services.AddScoped<IChannelVariablesRepository, ChannelVariablesRepository>();
        services.AddScoped<IWidgetConfigurationsRepository<TTSWidgetConfiguration>, TTSWidgetConfigurationsRepository>();

        services.AddScoped<IBuiltInCommandsService, BuiltInCommandsService>();
        services.AddScoped<ICommandsService, CommandsService>();

        services.AddSingleton<IChannelMessageCacheService, ChannelMessageCacheService>();

        services.AddHealthChecks()
        .AddNpgSql(connectionString, name: "PostgreSQL DB", timeout: TimeSpan.FromSeconds(30), tags: new[] { DB_READY });

        return services;
    }

    public static IApplicationBuilder UseChatbotEndpoints(this IApplicationBuilder app)
    {
        app.UseRouting();

        return app.UseEndpoints(endpoints =>
        {
            endpoints.MapControllers();

            endpoints.MapHealthChecks("/health/ready", new HealthCheckOptions
            {
                Predicate = (check) => check.Tags.Contains(DB_READY),
                ResponseWriter = async (context, report) =>
                {
                    var result = JsonSerializer.Serialize(
                        new
                        {
                            status = report.Status.ToString(),
                            checks = report.Entries.Select(entry => new
                            {
                                name = entry.Key,
                                status = entry.Value.Status.ToString(),
                                exception = entry.Value.Exception?.Message ?? "none",
                                duration = entry.Value.Duration.ToString()
                            })
                        }
                    );

                    context.Response.ContentType = MediaTypeNames.Application.Json;
                    await context.Response.Body.WriteAsync(Encoding.UTF8.GetBytes(result));
                }
            });
            endpoints.MapHealthChecks("/health/live", new HealthCheckOptions
            {
                Predicate = _ => false
            });
        });
    }

    /// <summary>
    /// Checks and performs any unapplied database migrations
    /// </summary>
    /// <param name="scope">The temporary Service Scope on application startup</param>
    public static async Task PerformDatabaseMigrations(this IServiceScope scope)
    {
        var context = scope.ServiceProvider.GetRequiredService<PostgreSqlContext>();
        var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();
        var ttsWidgetConfigurationsRepository = scope.ServiceProvider.GetRequiredService<IWidgetConfigurationsRepository<TTSWidgetConfiguration>>();

        context.Database.Migrate();

        var channels = await channelsRepository.GetAllAsync();
        foreach (var channel in channels)
        {
            var ttsWidgetConfiguration = await ttsWidgetConfigurationsRepository.GetByChannelIdAsync(channel.Id);
            if (ttsWidgetConfiguration is null)
            {
                await ttsWidgetConfigurationsRepository.AddAsync(channel.Id);
            }
        }
    }
}
