namespace ArchieMate.TwitchIRC.UserTypes;

public enum Types
{
    NormalUser,
    Moderator,
    TwitchAdmin,
    GlobalModerator,
    TwitchEmployee,
}

public static class Decoder
{
    public static Types Decode(string userType)
    {
        switch (userType)
        {
            case "":
                return Types.NormalUser;
            case "admin":
                return Types.TwitchAdmin;
            case "global_mod":
                return Types.GlobalModerator;
            case "staff":
                return Types.TwitchEmployee;
            case "mod":
                return Types.Moderator;
        }

        throw new ArgumentException($"Invalid user type {userType}");
    }
}
