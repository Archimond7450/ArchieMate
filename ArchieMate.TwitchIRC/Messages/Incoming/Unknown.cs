using System.Text.RegularExpressions;

namespace ArchieMate.TwitchIRC.Messages.Incoming;

public class Unknown : Message
{
    private static readonly GroupCollection groupCollection = Regex.Match("", "").Groups;
    public string Message { get; init; }

    public Unknown(string message) : base(groupCollection)
    {
        this.Message = message;
    }
}