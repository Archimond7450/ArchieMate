using ArchieMate.Chatbot.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Backend.Controllers;

[ApiController]
[Route("api/chatbot")]
public class ChatbotController : ControllerBase
{
    private readonly ILogger<ChatbotController> logger;
    private readonly TwitchIRCService chatbotService;

    public ChatbotController(ILogger<ChatbotController> logger, TwitchIRCService chatbotService)
    {
        this.logger = logger;
        this.chatbotService = chatbotService;
    }

    [HttpGet("join")]
    public async Task<IActionResult> Join([FromQuery] string channelName)
    {
        await this.chatbotService.JoinChannelAsync(channelName);

        return await Task.FromResult(NoContent());
    }

    [HttpGet("part")]
    public async Task<IActionResult> Part([FromQuery] string channelName)
    {
        await this.chatbotService.LeaveChannelAsync(channelName);

        return await Task.FromResult(NoContent());
    }

    [HttpGet("status")]
    public IActionResult Status()
    {
        return Ok(this.chatbotService.GetActiveChannels());
    }
}
