using ArchieMate.Chatbot.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Backend.Controllers;

[ApiController]
[Route("api/chatbot")]
public class ChatbotController : ControllerBase
{
    private readonly ILogger<ChatbotController> logger;
    private readonly TwitchIRCService twitchIRCService;

    public ChatbotController(ILogger<ChatbotController> logger, TwitchIRCService twitchIRCService)
    {
        this.logger = logger;
        this.twitchIRCService = twitchIRCService;
    }

    [HttpGet("join")]
    public async Task<IActionResult> Join([FromQuery] string channelName)
    {
        await this.twitchIRCService.JoinChannelAsync(channelName);

        return await Task.FromResult(NoContent());
    }

    [HttpGet("part")]
    public async Task<IActionResult> Part([FromQuery] string channelName)
    {
        await this.twitchIRCService.LeaveChannelAsync(channelName);

        return await Task.FromResult(NoContent());
    }

    [HttpGet("status")]
    public IActionResult Status()
    {
        return Ok(this.twitchIRCService.GetActiveChannels());
    }
}
