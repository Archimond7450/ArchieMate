namespace ArchieMate.TwitchIRC;

public static class Emotes
{
    public record Info
    {
        public string Id { get; init; } = string.Empty;
        public int Position { get; init; }
        public int EmoteTextLength { get; init; }
    }
    public static List<Info> Decode(string emotes)
    {
        var result = new List<Info>();

        if (emotes.Length > 0)
        {
            var splittedEmotes = emotes.Split('/');
            foreach (var oneEmote in splittedEmotes)
            {
                var colonPosition = oneEmote.IndexOf(':');

                var id = oneEmote.Substring(0, colonPosition);
                var positions = oneEmote.Substring(colonPosition + 1);
                var splittedPositions = positions.Split(',');
                foreach (var onePosition in splittedPositions)
                {
                    var dashPosition = onePosition.IndexOf('-');
                    var strPositionStart = onePosition.Substring(0, dashPosition);
                    var positionStart = Convert.ToInt32(strPositionStart);
                    var strPositionEnd = onePosition.Substring(dashPosition + 1);
                    var positionEnd = Convert.ToInt32(strPositionEnd) + 1;
                    var emoteTextLength = positionEnd - positionStart;

                    Info oneInfo = new Info
                    {
                        Id = id,
                        Position = positionStart,
                        EmoteTextLength = emoteTextLength
                    };

                    result.Add(oneInfo);
                }
            }
        }

        return result;
    }
}