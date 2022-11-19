using System.Text;
using System.Text.RegularExpressions;
using ArchieMate.Chatbot.Services.Database.Repositories;
using ArchieMate.TwitchIRC.Messages.Incoming;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Services;

public class CommandsService : ICommandsService
{
    private static class BuiltInVariables
    {
        internal static class Time
        {
            internal const string Name = "time";

            internal static string GetResponse(string format = "HH:mm:ss")
            {
                return DateTimeOffset.Now.ToString(format);
            }
        }

        internal static class Chatters
        {
            internal const string Name = "chatters";

            internal static string GetResponse(CommandDetail commandDetail, string? separator = null)
            {
                if (separator is null) separator = ", ";
                return (commandDetail.Chatters.Length > 0) ? String.Join(separator, commandDetail.Chatters) : "";
            }
        }

        internal static class Sender
        {
            internal const string Name = "sender";

            internal static string GetResponse(PrivMsg msg)
            {
                return msg.DisplayName;
            }
        }

        internal static class Params
        {
            internal const string Name = "params";

            internal static string GetResponse(CommandDetail commandDetail, string? action = null)
            {
                bool useChatters = commandDetail.Parameters.Length == 0;
                var source = (useChatters) ? commandDetail.Chatters : commandDetail.Parameters.Split();

                if (action is string strAction)
                {
                    switch (strAction)
                    {
                        case "random":
                            return source[Random.Shared.Next(source.Length)];

                        default:
                            return ActionNotCorrectMessage;
                    }
                }

                return (useChatters) ? String.Join(", ", source) : commandDetail.Parameters;
            }
        }

        internal static class Targets
        {
            internal const string Name = "targets";

            internal static string GetResponse(CommandDetail commandDetail)
            {
                return String.Join(", ", commandDetail.Chatters);
            }
        }
    }

    private const string VariableNotFoundMessage = "#VARIABLE_NOT_FOUND";
    private const string VariableFormattingNotCorrectMessage = "#VARIABLE_FORMATING_NOT_CORRECT";
    private const string ActionNotCorrectMessage = "#ACTION_NOT_CORRRECT";
    private static readonly Regex regularExpression = new Regex(@"^(?'chatters1'(@\w+\s*,??\s*?)*?)!(?'command'\w+)\s*((?'chatters2'(@\w+\s*?,??\s*)*)(\s(?'parameters'.*)?)?)?$");

    private readonly ILogger logger;
    private readonly IChannelVariablesRepository channelVariablesRepository;

    public CommandsService(ILogger<CommandsService> logger, IChannelVariablesRepository channelVariablesRepository)
    {
        this.logger = logger;
        this.channelVariablesRepository = channelVariablesRepository;
    }

    public CommandDetail? DecodeCommand(string command)
    {
        if (regularExpression.Match(command) is Match match && match.Length > 0)
        {
            var chatters = new StringBuilder();
            if (match.Groups.TryGetValue("chatters1", out var chatters1))
            {
                chatters.Append(chatters1.Value);
            }
            if (match.Groups.TryGetValue("chatters2", out var chatters2))
            {
                if (chatters.Length > 0 && chatters2.Length > 0)
                    chatters.Append(",");

                chatters.Append(chatters2.Value);
            }
            chatters.Replace(" ", "");

            var parameters = string.Empty;
            if (match.Groups.TryGetValue("parameters", out var parametersGroup))
            {
                parameters = parametersGroup.Value;
            }

            return new CommandDetail
            {
                Chatters = (chatters.Length > 0) ? chatters.ToString().Split(',') : new string[] { },
                CommandName = match.Groups["command"].Value,
                Parameters = parameters
            };
        }

        return null;
    }

    private enum ExpandVariablesStateMachineState
    {
        Initial,
        VariableStart,
        EscapeStart,
        BuiltInVariable,
        ChannelVariable,
    }

    public async Task<string> ExpandVariablesAsync(string response, PrivMsg privMsg, CommandDetail commandDetail)
    {
        var responseBuilder = new StringBuilder();
        var variableNameBuilder = new StringBuilder();
        var channelVariableSurrounded = false;

        var state = ExpandVariablesStateMachineState.Initial;

        foreach (var character in response)
        {
            switch (state)
            {
                case ExpandVariablesStateMachineState.Initial:
                    switch (character)
                    {
                        case '$':
                            state = ExpandVariablesStateMachineState.VariableStart;
                            break;

                        case '\\':
                            state = ExpandVariablesStateMachineState.EscapeStart;
                            break;

                        default:
                            responseBuilder.Append(character);
                            break;

                    }
                    break;

                case ExpandVariablesStateMachineState.EscapeStart:
                    switch (character)
                    {
                        case '$':
                            responseBuilder.Append(character);
                            break;

                        default:
                            responseBuilder.Append('\\');
                            responseBuilder.Append(character);
                            break;
                    }
                    state = ExpandVariablesStateMachineState.Initial;
                    break;

                case ExpandVariablesStateMachineState.VariableStart:
                    switch (character)
                    {
                        case '{':
                            variableNameBuilder.Clear();
                            state = ExpandVariablesStateMachineState.BuiltInVariable;
                            break;

                        case '[':
                            variableNameBuilder.Clear();
                            channelVariableSurrounded = true;
                            state = ExpandVariablesStateMachineState.ChannelVariable;
                            break;

                        case ' ':
                            variableNameBuilder.Clear();
                            responseBuilder.Append('$');
                            responseBuilder.Append(character);
                            state = ExpandVariablesStateMachineState.Initial;
                            break;

                        default:
                            variableNameBuilder.Clear();
                            state = ExpandVariablesStateMachineState.ChannelVariable;
                            variableNameBuilder.Append(character);
                            break;
                    }
                    break;

                case ExpandVariablesStateMachineState.BuiltInVariable:
                    switch (character)
                    {
                        case ' ':
                            responseBuilder.Append($"${{{variableNameBuilder.ToString()} ");
                            state = ExpandVariablesStateMachineState.Initial;
                            break;

                        case '}':
                            responseBuilder.Append(this.ExpandBuiltInVariable(variableNameBuilder.ToString(), privMsg, commandDetail));
                            state = ExpandVariablesStateMachineState.Initial;
                            break;

                        default:
                            variableNameBuilder.Append(character);
                            break;
                    }
                    break;

                case ExpandVariablesStateMachineState.ChannelVariable:
                    switch (character)
                    {
                        case ' ':
                            if (channelVariableSurrounded)
                            {
                                responseBuilder.Append($"$[{variableNameBuilder} ");
                            }
                            else
                            {
                                responseBuilder.Append(this.ExpandChannelVariableAsync(variableNameBuilder.ToString(), privMsg));
                                responseBuilder.Append(' ');
                            }
                            state = ExpandVariablesStateMachineState.Initial;
                            break;

                        case ']':
                            responseBuilder.Append(await this.ExpandChannelVariableAsync(variableNameBuilder.ToString(), privMsg));
                            state = ExpandVariablesStateMachineState.Initial;
                            break;

                        default:
                            variableNameBuilder.Append(character);
                            break;
                    }
                    break;
            }
        }

        return responseBuilder.ToString();
    }

    private string ExpandBuiltInVariable(string variableName, PrivMsg privMsg, CommandDetail commandDetail)
    {
        var afterColon = (variableName.IndexOf(':') > 0) ? variableName.Substring(variableName.IndexOf(':') + 1) : null;

        if (variableName.StartsWith(BuiltInVariables.Time.Name, StringComparison.OrdinalIgnoreCase))
        {
            if (variableName.Length > BuiltInVariables.Time.Name.Length)
            {
                var parts = variableName.Split(':');
                if (parts.Length == 2)
                {
                    return BuiltInVariables.Time.GetResponse(parts[1]);
                }
                return VariableFormattingNotCorrectMessage;
            }

            return BuiltInVariables.Time.GetResponse();
        }

        if (variableName.StartsWith(BuiltInVariables.Chatters.Name, StringComparison.OrdinalIgnoreCase))
        {
            if (variableName.Length > BuiltInVariables.Chatters.Name.Length)
            {
                return BuiltInVariables.Chatters.GetResponse(commandDetail, afterColon);
            }

            return BuiltInVariables.Chatters.GetResponse(commandDetail);
        }

        if (String.Equals(variableName, BuiltInVariables.Sender.Name, StringComparison.OrdinalIgnoreCase))
        {
            return BuiltInVariables.Sender.GetResponse(privMsg);
        }

        if (variableName.StartsWith(BuiltInVariables.Params.Name, StringComparison.OrdinalIgnoreCase) && (commandDetail.Chatters.Length > 0 || commandDetail.Parameters.Length > 0))
        {
            return BuiltInVariables.Params.GetResponse(commandDetail, afterColon);
        }

        if (String.Equals(variableName, BuiltInVariables.Targets.Name, StringComparison.OrdinalIgnoreCase))
        {
            return BuiltInVariables.Targets.GetResponse(commandDetail);
        }

        return VariableNotFoundMessage;
    }

    private async Task<string> ExpandChannelVariableAsync(string variableName, PrivMsg privMsg)
    {
        var channelVariable = await this.channelVariablesRepository.GetByChannelAndNameAsync(privMsg.Channel, variableName);
        return (channelVariable is null) ? VariableNotFoundMessage : channelVariable.Value;
    }
}
