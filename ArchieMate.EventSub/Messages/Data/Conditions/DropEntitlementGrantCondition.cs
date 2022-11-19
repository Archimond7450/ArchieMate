using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record DropEntitlementGrantCondition : ConditionData
{
    [JsonRequired]
    [JsonPropertyName("organization_id")]
    public string OrganizationId { get; init; } = String.Empty;
    [JsonPropertyName("category_id")]
    public string? CategoryId { get; init; } = null;
    [JsonPropertyName("campaign_id")]
    public string? CampaignId { get; init; } = null;
}
