using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record HypeTrainEndCondition : ConditionData
{
    [JsonRequired]
    [JsonPropertyName("broadcaster_user_id")]
    public string BroadcasterUserId { get; init; } = String.Empty;
}
