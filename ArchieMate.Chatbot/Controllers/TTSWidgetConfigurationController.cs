using ArchieMate.Chatbot.Models;
using ArchieMate.Chatbot.Models.DTOs.Incoming;
using ArchieMate.Chatbot.Services.Database.Repositories;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Backend.Controllers;

[ApiController]
[Route("api/configuration/widgets/tts/{channelId}")]
public class TTSWidgetConfigurationController : ControllerBase
{
    private readonly ILogger<WidgetsConfigurationController> logger;
    private readonly IWidgetConfigurationsRepository<TTSWidgetConfiguration> ttsWidgetConfigurationsRepository;

    public TTSWidgetConfigurationController(ILogger<WidgetsConfigurationController> logger, IWidgetConfigurationsRepository<TTSWidgetConfiguration> ttsConfRepo)
    {
        this.logger = logger;
        this.ttsWidgetConfigurationsRepository = ttsConfRepo;
    }

    [HttpPut]
    public async Task<IActionResult> UpdateAsync(Guid channelId, UpdateTTSWidgetConfigurationDTO configDTO)
    {
        var config = await this.ttsWidgetConfigurationsRepository.GetByChannelIdAsync(channelId);

        if (config is null)
        {
            return NotFound();
        }

        var updatedConfig = configDTO.ToModel(config.Id, channelId);

        await this.ttsWidgetConfigurationsRepository.UpdateAsync(updatedConfig);

        return NoContent();
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
