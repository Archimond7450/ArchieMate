using System.Text.Json.Serialization;

namespace ArchieMate.EventSub.Messages.Data;

public record TransportData
{
    [JsonRequired]
    [JsonPropertyName("method")]
    public string Method { get; init; } = String.Empty;
    [JsonRequired]
    [JsonPropertyName("callback")]
    public string Callback { get; init; } = String.Empty;
}