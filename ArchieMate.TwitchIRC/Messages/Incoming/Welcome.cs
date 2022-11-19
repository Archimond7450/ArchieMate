namespace ArchieMate.TwitchIRC.Messages.Incoming;


using System.Text.RegularExpressions;

public class Welcome : Message
{
    private static readonly Regex regularExpression = new Regex(@":tmi\.twitch\.tv\s(?'code'\d{3})\s(?'user'\S+)\s:(?'message'.*)");

    public int Code { get; init; }
    public string User { get; init; }
    public string Message { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);
        
        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public Welcome(GroupCollection groups) : base(groups)
    {
        this.Code = Convert.ToInt32(groups["code"].Value);
        this.User = groups["user"].Value;
        this.Message = groups["message"].Value;
    }
}