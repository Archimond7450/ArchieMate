using System;
using System.Globalization;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using System.Security.Cryptography;
using System.Diagnostics;
using ArchieMate.Chatbot.Models;
using ArchieMate.Chatbot.Models.DTOs.Incoming;
using ArchieMate.Chatbot.Options;
using ArchieMate.Chatbot.Services.Database.Repositories;
using ArchieMate.TwitchIRC;
using ArchieMate.TwitchIRC.Messages.Incoming;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace ArchieMate.Chatbot.Services;

public class ChatbotService : BackgroundService
{
    private IRC irc;
    private readonly ILogger<ChatbotService> logger;
    private readonly IServiceProvider serviceProvider;
    private readonly AuthOptions authOptions;

    public ChatbotService(ILogger<ChatbotService> logger, IServiceProvider serviceProvider, IOptions<AuthOptions> authOptions)
    {
        this.logger = logger;

        this.logger.LogInformation("Initializing Chatbot service");

        this.serviceProvider = serviceProvider;
        this.authOptions = authOptions.Value;
        this.irc = new IRC(this.logger, this.authOptions.Username, this.authOptions.Token);
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            await this.irc.ConnectAsync();

            using (var scope = this.serviceProvider.CreateScope())
            {
                var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();
                if (channelsRepository.GetByNameAsync(this.authOptions.Username).Result is null)
                {
                    await channelsRepository.AddAsync(new Channel
                    {
                        Name = this.authOptions.Username,
                        Join = true
                    });
                }
            }

            using (var scope = this.serviceProvider.CreateScope())
            {
                var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();

                foreach (var channel in await channelsRepository.GetAllAsync())
                {
                    if (channel.Join)
                    {
                        await this.irc.JoinChannelAsync(channel.Name);
                    }
                }
            }

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var message = await this.irc.ReceiveAsync();
                    if (message is PrivMsg privMsg)
                    {
                        using (var scope = this.serviceProvider.CreateScope())
                        {
                            var commandsService = scope.ServiceProvider.GetRequiredService<ICommandsService>();
                            var builtInCommandsService = scope.ServiceProvider.GetRequiredService<IBuiltInCommandsService>();
                            var commandsRepository = scope.ServiceProvider.GetRequiredService<ICommandsRepository>();
                            var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();

                            if (commandsService.DecodeCommand(privMsg.Message) is CommandDetail commandDetail)
                            {
                                var response = string.Empty;
                                if (await builtInCommandsService.HandleCommandAsync(commandDetail, privMsg) is string builtInCommandResponse)
                                {
                                    response = builtInCommandResponse;
                                }
                                else
                                {
                                    if (await commandsRepository.GetByChannelAndNameAsync(privMsg.Channel, commandDetail.CommandName) is Command commandFromDb)
                                    {
                                        response = commandFromDb.Response;
                                    }
                                }
                                if (response is string r && r.Length > 0)
                                {
                                    await this.irc.SendMessageAsync(privMsg.Channel, await commandsService.ExpandVariablesAsync(r, privMsg, commandDetail));
                                }
                            }
                        }
                    }
                    else if (message is Ping pingMsg)
                    {
                        await this.irc.SendPongAsync();
                    }
                    else if (message is RoomState roomStateMsg)
                    {
                        using (var scope = this.serviceProvider.CreateScope())
                        {
                            var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();

                            if (await channelsRepository.GetByNameAsync(roomStateMsg.Channel) is Channel channel)
                            {
                                await channelsRepository.UpdateAsync(
                                    channel with
                                    {
                                        Name = roomStateMsg.Channel,
                                        RoomId = roomStateMsg.RoomId
                                    }
                                );
                            }
                            else
                            {
                                this.logger.LogError($"Channel {roomStateMsg.Channel} is not in DB!");
                            }
                        }
                    }
                    else if (message is Reconnect reconnectMsg)
                    {
                        break;
                    }
                }
                catch (Exception ex)
                {
                    ex.Demystify();
                    this.logger.LogError(ex, "ChatbotService EXCEPTION!!!");
                }
            }

            if (!stoppingToken.IsCancellationRequested)
            {
                this.irc.Dispose();
                this.irc = new IRC(this.logger, this.authOptions.Username, this.authOptions.Token);
            }
        }
    }

    public async Task JoinChannelAsync(string channelName)
    {
        using (var scope = this.serviceProvider.CreateScope())
        {
            var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();

            if (await channelsRepository.GetByNameAsync(channelName) is Channel channel)
            {
                await channelsRepository.UpdateAsync(channel with { Join = true });
            }
            else
            {
                var dto = new CreateChannelDTO { Name = channelName };
                var model = dto.ToModel();
                await channelsRepository.AddAsync(model);
            }
        }

        await this.irc.JoinChannelAsync(channelName);
        //await this.irc.SendMessageAsync(channelName, "ArchieMate joined! VoHiYo");
    }

    public async Task LeaveChannelAsync(string channelName)
    {
        using (var scope = this.serviceProvider.CreateScope())
        {
            var channelsRepository = scope.ServiceProvider.GetRequiredService<IChannelsRepository>();

            if (await channelsRepository.GetByNameAsync(channelName) is Channel channel)
            {
                await channelsRepository.UpdateAsync(channel with { Join = false });
            }
            else
            {
                this.logger.LogError($"Channel {channelName} is not in DB!");
            }
        }

        //await this.irc.SendMessageAsync(channelName, "ArchieMate is leaving! MrDestructoid");
        await this.irc.LeaveChannelAsync(channelName);
    }

    public async Task SendMessageAsync(string channelName, string message)
    {
        await this.irc.SendMessageAsync(channelName, message);
    }

    public List<string> GetActiveChannels()
    {
        return this.irc.GetActiveChannels();
    }
}
