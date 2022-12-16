namespace ArchieMate.Chatbot.Models.DTOs.Outgoing
{
    public record ChannelMessageDTO
    {
        public required Guid MessageId { get; init; }
        public required string DisplayName { get; set; }
        public required string Message { get; init; }
    }
}