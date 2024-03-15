using System.Text;
using ArchieMate.Chatbot.Models;
using ArchieMate.Chatbot.Services;
using ArchieMate.Chatbot.Services.Database.Repositories;
using TwitchMessage = ArchieMate.TwitchIRC.Messages.Incoming.Message;

namespace ArchieMate.Chatbot.UnitTests.Services
{
    public class CommandsExecution
    {
        public record TestCase
        {
            public string CommandName { get; init; } = String.Empty;
            public string CommandResponse { get; init; } = String.Empty;
            public string CommandParameters { get; init; } = String.Empty;
            public string ExpectedResponse { get; init; } = String.Empty;
        }

        public static readonly TestCase TestCaseTime = new TestCase
        {
            CommandName = "time",
            CommandResponse = "${time}",
            CommandParameters = "",
            ExpectedResponse = ""
        };

        IChannelVariablesRepository channelVariablesRepository;
        ICommandsRepository commandsRepository;
        IChannelsRepository channelsRepository;
        ICommandsService commandsService;
        IBuiltInCommandsService builtInCommandsService;

        public CommandsExecution()
        {
            var channelVariablesRepositoryLogger = Substitute.For<ILogger<IChannelVariablesRepository>>();
            var commandsServiceLogger = Substitute.For<ILogger<CommandsService>>();
            var builtInCommandsServiceLogger = Substitute.For<ILogger<BuiltInCommandsService>>();

            this.commandsRepository = Substitute.For<ICommandsRepository>();
            this.channelsRepository = Substitute.For<IChannelsRepository>();
            this.channelVariablesRepository = Substitute.For<IChannelVariablesRepository>();
            this.commandsService = new CommandsService(commandsServiceLogger, channelVariablesRepository);
            this.builtInCommandsService = new BuiltInCommandsService(builtInCommandsServiceLogger, this.channelsRepository, commandsRepository);
        }

        [Fact]
        public async Task TimeWithoutFormatIsProperlyReturned()
        {
            // Arrange
            var channelGuid = Guid.NewGuid();
            var channelName = new StringBuilder(channelGuid.ToString()).Replace("-", "").ToString().ToLower();

            var channel = new Channel
            {
                Name = channelName,
                Join = true
            };

            IEnumerable<ChannelVariable> channelVariables = new List<ChannelVariable>() { };
            this.channelVariablesRepository.GetAllAsync().Returns(channelVariables);

            var command = new Command
            {
                ChannelId = channelGuid,
                Channel = channel,
                Name = TestCaseTime.CommandName,
                Response = TestCaseTime.CommandResponse
            };

            var channelNameInMessage = "#" + channel.Name;
            var commandUsage = $"!{TestCaseTime.CommandName}";
            var receivedMessage = $"@badge-info=subscriber/26;badges=subscriber/24,premium/1;client-nonce=01eb7004786255bc78e2c60676b79eaf;color=#00FF00;display-name=Archimond7450;emotes=;first-msg=0;flags=;id=8c4890a8-27a4-47ac-b114-e217105325f3;mod=0;room-id=23693840;subscriber=1;tmi-sent-ts=1649338601307;turbo=0;user-id=147113965;user-type= :archimond7450!archimond7450@archimond7450.tmi.twitch.tv PRIVMSG {channelNameInMessage} :{commandUsage}";
            var decodedMsg = TwitchMessage.Decode(receivedMessage);
            PrivMsg privMsg;

            if (decodedMsg is PrivMsg msg)
            {
                privMsg = msg;
            }
            else
            {
                decodedMsg.Should().BeOfType<PrivMsg>();
                return;
            }

            this.channelsRepository.GetByNameAsync(channelName).Returns(channel);
            this.commandsRepository.GetByChannelAndNameAsync(channel.Name, TestCaseTime.CommandName).Returns(command);

            // Act
            var commandDetail = this.commandsService.DecodeCommand(privMsg.Message);
            if (commandDetail is null)
            {
                commandDetail.Should().BeOfType<CommandDetail>();
                return;
            }

            var builtInCommandResponse = await this.builtInCommandsService.HandleCommandAsync(commandDetail, privMsg);
            if (builtInCommandResponse is not null)
            {
                builtInCommandResponse.Should().BeNull();
                return;
            }

            var commandFromDb = await this.commandsRepository.GetByChannelAndNameAsync(privMsg.Channel, commandDetail.CommandName);
            if (commandFromDb is null)
            {
                commandFromDb.Should().BeOfType<Command>();
                return;
            }

            var response = commandFromDb.Response;
            string finalResponse = "";
            if (response is string r && r.Length > 0)
            {
                while (DateTimeOffset.Now.Millisecond > 50)
                {
                    await Task.Delay(25);
                }

                finalResponse = await commandsService.ExpandVariablesAsync(r, privMsg, commandDetail);
            }

            // Assert
            commandDetail.Should().BeOfType<CommandDetail>();
            builtInCommandResponse.Should().BeNull();
            commandFromDb.Should().BeOfType<Command>();

            var expectedFinalResponse = DateTimeOffset.Now.ToString("HH:mm:ss");
            finalResponse.Should().Be(expectedFinalResponse);
        }
    }
}