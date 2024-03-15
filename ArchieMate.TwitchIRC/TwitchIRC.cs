using ArchieMate.TwitchIRC.Messages.Incoming;
using Microsoft.Extensions.Logging;

namespace ArchieMate.TwitchIRC;

public class IRC : IDisposable
{
    private ILogger logger;
    private readonly string host = "irc.chat.twitch.tv";
    private const int port = 6667;
    private readonly string username;
    private readonly string token;
    private readonly List<string> activeChannels;

    internal IRCSocket socket;

    public IRC(ILogger logger, string username, string token)
    {
        this.logger = logger;

        this.logger.LogDebug($"Initializing Twitch IRC with username: {username}, token length: {token.Length}");

        this.username = username;
        this.token = token;

        this.activeChannels = new List<string>();
    }

    public async Task ConnectAsync()
    {
        int delay = 1;

        while (delay != 0)
        {
            try
            {
                this.socket = new(this.logger, host, port);

                // Authentication
                await this.socket.SendAsync($"PASS oauth:{this.token}");
                await this.socket.SendAsync($"NICK {this.username}");

                // Receive welcome message
                if (await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 001 {this.username} :Welcome, GLHF!" ||
                    await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 002 {this.username} :Your host is tmi.twitch.tv" ||
                    await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 003 {this.username} :This server is rather new" ||
                    await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 004 {this.username} :-" ||
                    await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 375 {this.username} :-" ||
                    await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 372 {this.username} :You are in a maze of twisty passages, all alike." ||
                    await this.socket.ReceiveNowAsync() != $":tmi.twitch.tv 376 {this.username} :>")
                {
                    throw new InvalidDataException("Twitch IRC didn't authenticate properly");
                }

                // Request all capabilities
                await this.socket.SendAsync($"CAP REQ :twitch.tv/membership twitch.tv/tags twitch.tv/commands");

                // Receive CAP ACK
                IMessage? msg = null;
                while (msg is not CapabilityAcknowledge)
                {
                    msg = await this.HandleOneMessageAsync(true);
                }

                if (msg is CapabilityAcknowledge ack && ack.Capabilities.Length == 3)
                {
                    if (ack.Capabilities[0] != "twitch.tv/membership" ||
                        ack.Capabilities[1] != "twitch.tv/tags" ||
                        ack.Capabilities[2] != "twitch.tv/commands")
                    {
                        throw new InvalidDataException("Invalid Twitch response for capabilities request");
                    }
                }

                // Join the chatbot's channel (if the chatbot doesn't join any channel, then Twitch disconnects the chatbot)
                await this.socket.SendAsync($"JOIN {this.username}");
            }
            catch (Exception e)
            {
                this.logger.LogError(e, "EXCEPTION!");
                await Task.Delay(delay * 1000);
                delay *= 2;
                continue;
            }
            delay = 0;
        }
    }

    public async Task JoinChannelAsync(string channel)
    {
        if (!this.activeChannels.Contains(channel) && channel != this.username)
        {
            await this.socket.SendAsync($"JOIN #{channel}");
            this.activeChannels.Add(channel);
        }
    }

    public async Task LeaveChannelAsync(string channel)
    {
        if (this.activeChannels.Contains(channel) && channel != this.username)
        {
            await this.socket.SendAsync($"PART #{channel}");
            this.activeChannels.Remove(channel);
        }
    }

    public async Task SendMessageAsync(string channel, string message)
    {
        if (this.activeChannels.Contains(channel))
        {
            await this.socket.SendAsync($"PRIVMSG #{channel} :{message}");
        }
    }

    public async Task<IMessage?> ReceiveAsync()
    {
        return await HandleOneMessageAsync();
    }

    private async Task<IMessage?> HandleOneMessageAsync(bool now = false)
    {
        string msg = await ((now) ? this.socket.ReceiveNowAsync() : this.socket.ReceiveAsync());

        if (msg != string.Empty)
        {
            return await Task.FromResult(Message.Decode(msg));
        }

        return await Task.FromResult<IMessage?>(null);
    }

    public void Dispose()
    {
        this.socket.Dispose();
    }

    public List<string> GetActiveChannels()
    {
        return this.activeChannels;
    }

    public async Task SendPongAsync()
    {
        await this.socket.SendAsync("PONG :tmi.twitch.tv");
    }
}
