public record CommandDTO
{
    public Guid Id { get; init; } = new Guid();
    public Guid ChannelId { get; init; } = Guid.NewGuid();
    public string Name { get; init; } = String.Empty;
    public string Response { get; init; } = String.Empty;
}

public record CommandDTOWithoutChannel
{
    public Guid Id { get; init; } = new Guid();
    public string Name { get; init; } = String.Empty;
    public string Response { get; init; } = String.Empty;
}