using ArchieMate.Chatbot.Models.DTOs.Outgoing;
using ArchieMate.Chatbot.Services.Cache;
using ArchieMate.TwitchIRC.Messages.Incoming;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace ArchieMate.Chatbot.Controllers
{
    [Route("/api/channelmessage")]
    public class ChannelMessageController : Controller
    {
        private readonly ILogger<ChannelMessageController> logger;
        private readonly IChannelMessageCacheService channelMessageCacheService;

        public ChannelMessageController(ILogger<ChannelMessageController> logger, IChannelMessageCacheService channelMessageCacheService)
        {
            this.channelMessageCacheService = channelMessageCacheService;
            this.logger = logger;
        }

        [HttpGet("{channelId}/last")]
        public ActionResult<ChannelMessageDTO> GetLastChannelMessage(Guid channelId)
        {
            if (this.channelMessageCacheService.GetLatestChannelMessage(channelId) is PrivMsg msg)
            {
                return Ok(new ChannelMessageDTO { MessageId = msg.MessageId, DisplayName = msg.DisplayName, Message = msg.Message });
            }

            return NoContent();
        }

        [HttpGet("{channelId}/from/{messageFromId}")]
        public ActionResult<IEnumerable<ChannelMessageDTO>> GetAllChannelMessagesFromCertainMessage(Guid channelId, Guid messageFromId)
        {
            if (this.channelMessageCacheService.GetChannelMessagesFrom(channelId, messageFromId) is IEnumerable<PrivMsg> messages)
            {
                if (messages.Count() == 0)
                {
                    return NoContent();
                }

                return Ok(messages.Select(msg => new ChannelMessageDTO { MessageId = msg.MessageId, DisplayName = msg.DisplayName, Message = msg.Message }));
            }

            return NotFound();
        }

        [HttpGet]
        public IActionResult GetAll()
        {
            return Ok(this.channelMessageCacheService.GetAll());
        }
    }
}