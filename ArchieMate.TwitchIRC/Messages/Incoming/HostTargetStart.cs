namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;


public class HostTargetStart : Message
{
    private static readonly Regex regularExpression = new Regex(@":tmi\.twitch\.tv\sHOSTTARGET\s#(?'hosting_channel'\S+)\s:(?'channel'\S{2,})\s(?'num_viewers'.*)");

    public string Channel { get; init; }
    public string HostedChannel { get; init; }
    public int NumViewers { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);
        
        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public HostTargetStart(GroupCollection groups) : base(groups)
    {
        this.Channel = groups["hosting_channel"].Value;
        this.HostedChannel = groups["channel"].Value;
        this.NumViewers = Convert.ToInt32(groups["num_viewers"].Value);
    }
}