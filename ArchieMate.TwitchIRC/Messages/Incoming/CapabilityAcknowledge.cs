namespace ArchieMate.TwitchIRC.Messages.Incoming;


using System.Text.RegularExpressions;

public class CapabilityAcknowledge : Message
{
    private static readonly Regex regularExpression = new Regex(@":tmi\.twitch\.tv\sCAP\s\*\sACK\s:(?'capabilities'.*)");

    public string[] Capabilities { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);
        
        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public CapabilityAcknowledge(GroupCollection groups) : base(groups)
    {
        string capabilities = groups["capabilities"].Value;
        this.Capabilities = capabilities.Split(" ");
    }
}