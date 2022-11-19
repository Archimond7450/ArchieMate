using ArchieMate.Chatbot.Services;
using ArchieMate.Chatbot.Services.Database.Repositories;
using FluentAssertions;
using Microsoft.Extensions.Logging;
using Moq;

namespace ArchieMate.Chatbot.UnitTests.Services;

public class CommandsServiceDecodeTests
{
    public const string NoCommand = "Hi";
    public const string JustCommand = "!time";
    public const string LeftChattersAndCommand = "@a, @z !hi";
    public const string RightChattersAndCommand = "!commands @t";
    public const string BothChattersWithoutParameters = "@x !r @z";
    public const string FullCommand = "@b, @d !test @x Something";

    private ICommandsService commandsService;

    public CommandsServiceDecodeTests()
    {
        var loggerMock = new Mock<ILogger<CommandsService>>();
        var channelVariablesRepositoryMock = new Mock<IChannelVariablesRepository>();
        this.commandsService = new CommandsService(loggerMock.Object, channelVariablesRepositoryMock.Object);
    }

    [Fact]
    public void DecodeSuccessfullyDetectsNoCommandAndReturnsNull()
    {
        // Arrange
        string cmd = NoCommand;

        // Act
        var result = this.commandsService.DecodeCommand(cmd);

        // Assert
        result.Should().BeNull();
    }

    [Fact]
    public void DecodeSuccessfullyJustCommandAndReturnsObject()
    {
        // Arrange
        string cmd = JustCommand;

        // Act
        var result = this.commandsService.DecodeCommand(cmd);

        // Assert
        result.Should().BeEquivalentTo(new CommandDetail
        {
            Chatters = new string[] { },
            CommandName = "time",
            Parameters = String.Empty
        });
    }

    [Fact]
    public void DecodeSuccessfullyLeftChattersAndCommandAndReturnsObject()
    {
        // Arrange
        string cmd = LeftChattersAndCommand;

        // Act
        var result = this.commandsService.DecodeCommand(cmd);

        // Assert
        result.Should().BeEquivalentTo(new CommandDetail
        {
            Chatters = new string[] { "@a", "@z" },
            CommandName = "hi",
            Parameters = String.Empty
        });
    }

    [Fact]
    public void DecodeSuccessfullyRightChattersAndCommandAndReturnsObject()
    {
        // Arrange
        string cmd = RightChattersAndCommand;

        // Act
        var result = this.commandsService.DecodeCommand(cmd);

        // Assert
        result.Should().BeEquivalentTo(new CommandDetail
        {
            Chatters = new string[] { "@t" },
            CommandName = "commands",
            Parameters = String.Empty
        });
    }

    [Fact]
    public void DecodeSuccessfullyBothChattersWithoutParametersAndReturnsObject()
    {
        // Arrange
        string cmd = BothChattersWithoutParameters;

        // Act
        var result = this.commandsService.DecodeCommand(cmd);

        // Assert
        result.Should().BeEquivalentTo(new CommandDetail
        {
            Chatters = new string[] { "@x", "@z" },
            CommandName = "r",
            Parameters = String.Empty
        });
    }

    [Fact]
    public void DecodeSuccessfullyFullCommandAndReturnsObject()
    {
        // Arrange
        string cmd = FullCommand;

        // Act
        var result = this.commandsService.DecodeCommand(cmd);

        // Assert
        result.Should().BeEquivalentTo(new CommandDetail
        {
            Chatters = new string[] { "@b", "@d", "@x" },
            CommandName = "test",
            Parameters = "Something"
        });
    }
}