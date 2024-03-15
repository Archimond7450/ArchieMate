using System.ComponentModel.Design;
using ArchieMate.Chatbot.Models.DTOs.Incoming;
using ArchieMate.Chatbot.Services.Database.Repositories;
using ArchieMate.Helpers;
using ArchieMate.TwitchIRC;
using ArchieMate.TwitchIRC.Messages.Incoming;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services;

public class BuiltInCommandsService : IBuiltInCommandsService
{
    private static class Command
    {
        internal const string Name = "command";
        internal static readonly string Usage = $"Usage: !command ({String.Join('|', Command.Types.All)}) [response]";
        internal static class Types
        {
            internal const string Add = "add";
            internal const string Create = "create";
            internal const string Edit = "edit";
            internal const string Update = "update";
            internal const string Change = "change";
            internal const string Delete = "delete";
            internal const string Remove = "remove";

            internal static readonly string[] All = {
                Add,
                Create,
                Edit,
                Update,
                Change,
                Delete,
                Remove
            };
            internal static readonly string[] AllAdd = {
                Add,
                Create
            };
            internal static readonly string[] AllEdit = {
                Edit,
                Update,
                Change
            };
            internal static readonly string[] AllDelete = {
                Delete,
                Remove
            };
        }
    }

    private static class Commands
    {
        internal const string Name = "commands";
    }

    private readonly ILogger<BuiltInCommandsService> logger;
    private readonly IChannelsRepository channelsRepository;
    private readonly ICommandsRepository commandsRepository;
    public BuiltInCommandsService(ILogger<BuiltInCommandsService> logger, IChannelsRepository channelsRepository, ICommandsRepository commandsRepository)
    {
        this.logger = logger;
        this.channelsRepository = channelsRepository;
        this.commandsRepository = commandsRepository;
    }

    public async Task<string?> HandleCommandAsync(CommandDetail commandDetail, PrivMsg privMsg)
    {
        var channel = privMsg.Channel;
        if (commandDetail is null)
        {
            return null;
        }

        if (
            (privMsg.Badges.ContainsKey(Badges.Moderator) || privMsg.Badges.ContainsKey(Badges.Broadcaster))
            && (String.Equals(commandDetail.CommandName, Command.Name, StringComparison.OrdinalIgnoreCase)))
        {
            if (commandDetail.Parameters.Length == 0)
            {
                return Command.Usage;
            }

            var type = commandDetail.Parameters.Substring(0, commandDetail.Parameters.IndexOf(' '));
            if (!ArrayHelpers.ContainsString(Command.Types.All, type, StringComparison.OrdinalIgnoreCase))
            {
                return Command.Usage;
            }

            var afterType = commandDetail.Parameters.Substring(commandDetail.Parameters.IndexOf(' ')).Trim();
            var command = afterType.Contains(' ') ? afterType.Substring(0, afterType.IndexOf(' ')) : afterType;
            var commandResponse = String.Empty;
            try
            {
                commandResponse = afterType.Substring(command.Length).Trim();
            }
            catch (ArgumentOutOfRangeException)
            {

            }

            if (command.StartsWith('!'))
            {
                command = command.Substring(1);
            }

            this.logger.LogDebug($"Called !command => type: {type}, command: {command}, commandResponse: {commandResponse}");

            if (ArrayHelpers.ContainsString(Command.Types.AllAdd, type, StringComparison.OrdinalIgnoreCase))
            {
                if (commandResponse.Length == 0)
                {
                    return $"@${{sender}}, cannot create command '!{command}' with an empty response!";
                }
                else if (await this.commandsRepository.GetByChannelAndNameAsync(channel, command) is null)
                {
                    var foundChannel = await this.channelsRepository.GetByNameAsync(channel);
                    if (foundChannel is null)
                    {
                        return $"@${{sender}}, cannot create command '!{command}' because of internal error!";
                    }
                    else
                    {
                        await this.commandsRepository.AddAsync(new CreateCommandDTO
                        {
                            ChannelId = foundChannel.Id,
                            Name = command,
                            Response = commandResponse
                        }.ToModel());
                        return $"@${{sender}}, command '!{command}' was successfully created.";
                    }
                }
                else
                {
                    return $"@${{sender}}, cannot create command '!{command}' because it already exists!";
                }
            }

            if (ArrayHelpers.ContainsString(Command.Types.AllEdit, type, StringComparison.OrdinalIgnoreCase))
            {
                if (commandResponse.Length == 0)
                {
                    return $"@${{sender}}, cannot edit command '!{command}' with an empty response!";
                }

                var foundCommand = await this.commandsRepository.GetByChannelAndNameAsync(channel, command);
                if (foundCommand is null)
                {
                    return $"@${{sender}}, cannot edit command '!{command}' because it doesn't exist!";
                }
                else
                {
                    await this.commandsRepository.UpdateAsync(foundCommand with
                    {
                        Response = commandResponse
                    });
                    return $"@${{sender}}, command '!{command}' was successfully edited.";

                }
            }

            if (ArrayHelpers.ContainsString(Command.Types.AllDelete, type, StringComparison.OrdinalIgnoreCase))
            {
                var foundCommand = await this.commandsRepository.GetByChannelAndNameAsync(channel, command);
                if (foundCommand is null)
                {
                    return $"@${{sender}}, cannot delete command '!{command}' because it doesn't exist!";
                }
                else
                {
                    await this.commandsRepository.DeleteAsync(foundCommand);
                    return $"@${{sender}}, command '!{command}' was successfully deleted.";
                }
            }
        }
        else if (String.Equals(commandDetail.CommandName, Commands.Name, StringComparison.OrdinalIgnoreCase))
        {
            return $"@${{sender}}, the list of available commands for this channel is available here: https://archiemate.com/tables/commands/{channel}";
        }

        return null;
    }
}
