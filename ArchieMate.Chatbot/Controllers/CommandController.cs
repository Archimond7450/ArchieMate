using ArchieMate.Chatbot.Models;
using ArchieMate.Chatbot.Services.Database.Repositories;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Backend.Controllers;

[ApiController]
[Route("api/commands")]
public class CommandController : ControllerBase
{
    private readonly ILogger<CommandController> logger;
    private readonly ICommandsRepository commandsRepository;

    public CommandController(ILogger<CommandController> logger, ICommandsRepository commandsRepository)
    {
        this.logger = logger;
        this.commandsRepository = commandsRepository;
    }

    [HttpGet("channel/{channelName}")]
    public async Task<IActionResult> GetByName(string channelName)
    {
        var commands = await this.commandsRepository.GetAllForChannelAsync(channelName);

        var result = commands.Select(c => c.ToDTOWithoutChannel());

        return await Task.FromResult(Ok(result));
    }
}
