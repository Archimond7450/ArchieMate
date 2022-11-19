using System.Text.Json;
using System.Text.Json.Serialization;
using ArchieMate.EventSub.Messages.Data;

namespace ArchieMate.EventSub.Messages;

public record ChallengeMessage
{
    public static ChallengeMessage? DeserializeJson(string str)
    {
        return JsonSerializer.Deserialize<ChallengeMessage>(str);
    }

    [JsonRequired]
    [JsonPropertyName("challenge")]
    public string Challenge { get; init; } = String.Empty;
    [JsonRequired]
    [JsonPropertyName("subscription")]
    public SubscriptionData Subscription { get; init; } = default!;
}
