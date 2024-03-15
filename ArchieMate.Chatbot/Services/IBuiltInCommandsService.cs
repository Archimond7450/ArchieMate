using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.Chatbot.Services;

public interface IBuiltInCommandsService
{
    public Task<string?> HandleCommandAsync(CommandDetail command, PrivMsg privMsg);
}