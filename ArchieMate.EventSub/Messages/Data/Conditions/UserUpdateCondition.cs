using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record UserUpdateCondition : ConditionData
{
    [JsonRequired]
    [JsonPropertyName("user_id")]
    public string UserId { get; init; } = String.Empty;
}
