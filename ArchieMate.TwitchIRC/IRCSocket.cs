using System.Net;
using System.Net.Sockets;
using Microsoft.Extensions.Logging;

namespace ArchieMate.TwitchIRC;

/// <summary>
/// Represents a wrapper around socket for Twitch IRC communication.
/// </summary>
internal struct IRCSocket : IDisposable
{
    private ILogger logger;
    private TcpClient client;
    private NetworkStream stream;
    private StreamReader reader;
    private StreamWriter writer;

    /// <summary>
    /// Initializes a new instance of the <see cref="IRCSocket"/> class
    /// </summary>
    /// <param name="address">Twitch IRC address</param>
    /// <param name="port">Twitch IRC port</param>
    public IRCSocket(ILogger logger, string address, int port)
    {
        this.logger = logger;

        this.logger.LogDebug("Initializing IRC Socket");

        this.client = new();
        this.client.Connect(address, port);

        if (!this.client.Connected)
        {
            this.logger.LogDebug("Could not connect to Twitch IRC");
            throw new IOException("Could not connect to Twitch IRC");
        }

        this.stream = this.client.GetStream();
        this.reader = new StreamReader(stream);
        this.writer = new StreamWriter(stream);
    }

    /// <summary>
    /// Sends a message to the Twitch IRC
    /// </summary>
    /// <param name="message">message to be sent</param>
    public async Task SendAsync(string message)
    {
        message = message.TrimEnd('\r', '\n');

        if (!message.StartsWith("PASS"))
            this.logger.LogDebug($"< {message}");

        await this.writer.WriteLineAsync(message);
        this.Flush();
    }

    /// <summary>
    /// Receives from the Twitch IRC
    /// </summary>
    /// <returns>String received from Twitch IRC</returns>
    public async Task<string> ReceiveAsync()
    {
        if (this.stream.DataAvailable)
        {
            return await this.OneReceiveAsync();
        }

        await Task.Delay(1000);

        return await Task.FromResult(string.Empty);
    }

    public async Task<string> ReceiveNowAsync()
    {
        return await this.OneReceiveAsync();
    }

    private async Task<string> OneReceiveAsync()
    {
        string result = string.Empty;
        while (result.Length == 0)
        {
            if (await this.reader.ReadLineAsync() is string line)
            {
                result = line;
            }
        }

        result = result.TrimEnd('\r', '\n');

        this.logger.LogDebug($"> {result}");

        return await Task.FromResult(result);
    }

    private void Flush()
    {
        this.writer.Flush();
    }

    public void Dispose()
    {
        this.reader.Dispose();
        this.writer.Dispose();
        this.stream.Dispose();
        this.client.Dispose();
    }
}
