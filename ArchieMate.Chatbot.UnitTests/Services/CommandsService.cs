using ArchieMate.Chatbot.Services;
using ArchieMate.Chatbot.Services.Database.Repositories;
using FluentAssertions;
using Microsoft.Extensions.Logging;
using NSubstitute;
using Xunit;

namespace ArchieMate.Chatbot.UnitTests;

public class CommandsServiceTests
{
    public const string NoCommand = "Hi";
    public const string JustCommand = "!time";
    public const string LeftChattersAndCommand = "@a, @z !hi";
    public const string RightChattersAndCommand = "!commands @t";
    public const string FullCommand = "@b, @d !test @x Something";

    private ICommandsService commandsService;

    public CommandsServiceTests()
    {
        var loggerMock = Substitute.For<ILogger<CommandsService>>();
        var channelVariablesRepositoryMock = Substitute.For<IChannelVariablesRepository>();
        this.commandsService = new CommandsService(loggerMock, channelVariablesRepositoryMock);
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