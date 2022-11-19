namespace ArchieMate.TwitchIRC;

public static class EmoteSets
{
    public static int[] Decode(string emoteSets)
    {
        string[] emoteSetsSplit = emoteSets.Split(',');
        var result = new int[emoteSetsSplit.Length];

        for (int i = 0; i < emoteSetsSplit.Length; i++)
        {
            result[i] = Convert.ToInt32(emoteSetsSplit[i]);
        }

        return result;
    }
}