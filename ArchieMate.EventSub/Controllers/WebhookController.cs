using ArchieMate.EventSub.Messages;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.EventSub.Controllers;

[ApiController]
[Route("webhooks/eventsub")]
public class WebhookController : ControllerBase
{
    private readonly ILogger<WebhookController> _logger;

    public WebhookController(ILogger<WebhookController> logger)
    {
        _logger = logger;
    }

    [HttpPost]
    public ActionResult<string> Post([FromBody] string body)
    {
        _logger.LogDebug("EventSub Webhook");

        string? messageType = null;

        try
        {
            messageType = Request.Headers[Constants.Headers.TwitchEventsubMessageType];
        }
        catch (Exception)
        {
            return BadRequest($"Header '{Constants.Headers.TwitchEventsubMessageType}' is missing in the request.");
        }

        switch (messageType)
        {
            case Constants.NotificationMessageTypes.Verification:
                var json = ChallengeMessage.DeserializeJson(body);

                if (json is ChallengeMessage challengeMessage)
                {
                    return Ok(challengeMessage.Challenge);
                }

                return BadRequest("Cannot parse the json in the request body.");
        }

        return Ok("Test");
    }
}
