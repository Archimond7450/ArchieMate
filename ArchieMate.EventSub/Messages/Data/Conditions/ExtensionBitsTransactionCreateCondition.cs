using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data.Conditions;

public record ExtensionBitsTransactionCreateCondition : ConditionData
{
    [JsonRequired]
    [JsonPropertyName("extension_client_id")]
    public string ExtensionClientId { get; init; } = String.Empty;
}
