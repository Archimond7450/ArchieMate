using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record UserAuthorizationGrantCondition : ConditionData
{
    [JsonRequired]
    [JsonPropertyName("client_id")]
    public string ClientId { get; init; } = String.Empty;
}
