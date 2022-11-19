using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record ChannelPointsCustomRewardRedemptionUpdateCondition : ConditionData
{
    [JsonRequired]
    [JsonPropertyName("broadcaster_user_id")]
    public string BroadcasterUserId { get; init; } = String.Empty;
    [JsonPropertyName("reward_id")]
    public string? RewardId { get; init; } = null;
}
