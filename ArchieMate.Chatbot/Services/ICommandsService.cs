using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.Chatbot.Services;

public interface ICommandsService
{
    public CommandDetail? DecodeCommand(string command);
    public Task<string> ExpandVariablesAsync(string response, PrivMsg privMsg, CommandDetail commandDetail);
}
