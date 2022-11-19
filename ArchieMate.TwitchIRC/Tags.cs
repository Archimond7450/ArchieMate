using System.Text;

namespace ArchieMate.TwitchIRC;

public static class Tags
{
    public static Dictionary<string, string> Decode(string tags)
    {
        Dictionary<string, string> dict = new();

        foreach (string tag in tags.Split(';'))
        {
            int splitPosition = tag.IndexOf('=');

            string key = tag.Substring(0, splitPosition);
            string value = tag.Substring(splitPosition + 1);

            dict.Add(key, value);
        }

        return dict;
    }

    public static string ParseStringValue(string value)
    {
        StringBuilder builder = new();

        bool specialCharacter = false;

        foreach (char c in value)
        {
            if (c == '\\')
            {
                specialCharacter = true;
                continue;
            }

            if (specialCharacter)
            {
                switch (c)
                {
                    case ':':
                    {
                        builder.Append(';');
                        break;
                    }
                    case 's':
                    {
                        builder.Append(' ');
                        break;
                    }
                    case '\\':
                    {
                        builder.Append('\\');
                        break;
                    }
                    case 'r':
                    {
                        builder.Append('\r');
                        break;
                    }
                    case 'n':
                    {
                        builder.Append('\n');
                        break;
                    }
                    default:
                    {
                        builder.Append(c);
                        break;
                    }
                }

                specialCharacter = false;
                continue;
            }

            builder.Append(c);
        }

        return builder.ToString();
    }

    public static bool ParseBooleanValue(string value)
    {
        return (value == "1");
    }
}
