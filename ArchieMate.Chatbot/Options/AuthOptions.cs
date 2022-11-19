namespace ArchieMate.Chatbot.Options;

public record AuthOptions
{
    public string Username { get; init; } = "archiemate";
    public string Token { get; init; } = String.Empty;
}
