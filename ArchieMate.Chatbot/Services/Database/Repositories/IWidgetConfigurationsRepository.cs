namespace ArchieMate.Chatbot.Services.Database.Repositories;

public interface IWidgetConfigurationsRepository<T>
{
    Task<T?> GetByChannelIdAsync(Guid channelId);
    Task AddAsync(Guid channelId);
    Task ResetAsync(Guid channelId);
    Task UpdateAsync(T config);
}
