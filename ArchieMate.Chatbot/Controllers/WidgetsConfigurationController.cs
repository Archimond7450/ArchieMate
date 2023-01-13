using ArchieMate.Chatbot.Models;
using ArchieMate.Chatbot.Models.DTOs.Outgoing;
using ArchieMate.Chatbot.Services.Database.Repositories;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Backend.Controllers;

[ApiController]
[Route("api/configuration/widgets/")]
public class WidgetsConfigurationController : ControllerBase
{
    private readonly ILogger<WidgetsConfigurationController> logger;
    private readonly IWidgetConfigurationsRepository<TTSWidgetConfiguration> ttsWidgetConfigurationsRepository;

    public WidgetsConfigurationController(ILogger<WidgetsConfigurationController> logger, IWidgetConfigurationsRepository<TTSWidgetConfiguration> ttsConfRepo)
    {
        this.logger = logger;
        this.ttsWidgetConfigurationsRepository = ttsConfRepo;
    }

    [HttpGet("{channelId}")]
    public async Task<IActionResult> GetByChannelIdAsync(Guid channelId)
    {
        var ttsWidgetConfiguration = await this.ttsWidgetConfigurationsRepository.GetByChannelIdAsync(channelId);

        if (ttsWidgetConfiguration is null)
        {
            return NotFound();
        }

        var widgetsConfiguration = new WidgetsConfigurationDTO
        {
            TextToSpeech = ttsWidgetConfiguration.ToDTO()
        };

        return Ok(widgetsConfiguration);
    }

    [HttpDelete]
    public async Task<IActionResult> ResetAsync(Guid channelId)
    {
        var config = await this.ttsWidgetConfigurationsRepository.GetByChannelIdAsync(channelId);

        if (config is null)
        {
            return NotFound();
        }

        await this.ttsWidgetConfigurationsRepository.ResetAsync(channelId);

        return NoContent();
    }
}
