namespace ArchieMate.Chatbot;

public record CommandDetail
{
    public string[] Chatters { get; init; } = default!;
    public string CommandName { get; init; } = String.Empty;
    public string Parameters { get; init; } = String.Empty;
}
