using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data;

public record SubscriptionCreationData
{
    [JsonRequired]
    [JsonPropertyName("id")]
    public Guid Id { get; init; } = Guid.NewGuid();
    [JsonRequired]
    [JsonPropertyName("status")]
    public string Status { get; init; } = String.Empty;
    [JsonRequired]
    [JsonPropertyName("type")]
    public string Type { get; init; } = String.Empty;
    [JsonRequired]
    [JsonPropertyName("version")]
    public string Version { get; init; } = String.Empty;
    [JsonRequired]
    [JsonPropertyName("cost")]
    public int Cost { get; init; } = 0;
    [JsonRequired]
    [JsonPropertyName("condition")]
    public ConditionData Condition { get; init; } = default!;
    [JsonRequired]
    [JsonPropertyName("transport")]
    public TransportCreationData Transport { get; init; } = default!;
    [JsonRequired]
    [JsonPropertyName("created_at")]
    public DateTime CreatedAt { get; init; } = DateTime.MinValue;
}