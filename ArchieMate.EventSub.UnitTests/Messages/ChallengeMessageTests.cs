using ArchieMate.EventSub.Controllers;
using ArchieMate.EventSub.Messages;
using ArchieMate.EventSub.Messages.Data;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using NSubstitute;
using Xunit;
using static ArchieMate.EventSub.Constants;

namespace ArchieMate.EventSub.UnitTests.Messages;

public class ChallengeTests
{
    private string challengeMessageFileContents;
    private ChallengeMessage? challengeMessageJson;

    public ChallengeTests()
    {
        using (var challengeMessageFile = InputFiles.GetInputFile("ChallengeMessage.json"))
        {
            this.challengeMessageFileContents = challengeMessageFile.ReadToEnd();
            this.challengeMessageJson = ChallengeMessage.DeserializeJson(challengeMessageFileContents);
            this.challengeMessageJson.Should().NotBeNull();
            this.challengeMessageJson.Should().BeOfType<ChallengeMessage>();
        }
    }

    [Fact]
    public void ChallengeMessageIsCorrectlyDecoded()
    {
        var challengeMessageObject = new ChallengeMessage
        {
            Challenge = "pogchamp-kappa-360noscope-vohiyo",
            Subscription = new SubscriptionData
            {
                Id = new Guid("f1c2a387-161a-49f9-a165-0f21d7a4e1c4"),
                Status = "webhook_callback_verification_pending",
                Type = "channel.follow",
                Version = "1",
                Cost = 1,
                Condition = new ConditionData
                {
                    //BroadcasterUserId = "12826"
                },
                Transport = new TransportData
                {
                    Method = "webhook",
                    Callback = "https://example.com/webhooks/callback"
                },
                CreatedAt = new DateTime(2019, 11, 16, 10, 11, 12, 123)
            }
        };

        challengeMessageJson.Should().BeEquivalentTo(challengeMessageObject);
    }

    [Fact]
    public void WhenChallengeMessageIsReceivedInControllerChallengeIsSuccessfullyReturned()
    {
        var httpContext = new DefaultHttpContext();
        httpContext.Request.Headers.Add(Headers.TwitchEventsubMessageType, NotificationMessageTypes.Verification);
        var mockLogger = Substitute.For<ILogger<WebhookController>>();
        var controller = new WebhookController(mockLogger)
        {
            ControllerContext = new ControllerContext()
            {
                HttpContext = httpContext
            }
        };

        var response = controller.Post(this.challengeMessageFileContents);

        response.Result.Should().BeOfType<OkObjectResult>();
        if (response.Result is OkObjectResult result)
        {
            result.Value.Should().Be(this.challengeMessageJson!.Challenge);
        }

    }
}