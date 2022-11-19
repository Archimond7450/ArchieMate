using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record ChannelRaidCondition : ConditionData
{
    [JsonPropertyName("from_broadcaster_user_id")]
    public string? FromBroadcasterUserId { get; init; } = null;
    [JsonPropertyName("to_broadcaster_user_id")]
    public string? ToBroadcasterUserId { get; init; } = null;
}
