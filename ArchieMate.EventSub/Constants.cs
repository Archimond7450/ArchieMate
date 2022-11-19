namespace ArchieMate.EventSub;

public static class Constants
{
    public const string AssemblyName = "ArchieMate.EventSub";

    public static class Headers
    {
        public const string TwitchEventsubMessageId = "Twitch-Eventsub-Message-Id";
        public const string TwitchEventsubMessageRetry = "Twitch-Eventsub-Message-Retry";
        public const string TwitchEventsubMessageType = "Twitch-Eventsub-Message-Type";
        public const string TwitchEventsubMessageSignature = "Twitch-Eventsub-Message-Signature";
        public const string TwitchEventsubMessageTimestamp = "Twitch-Eventsub-Message-Timestamp";
        public const string TwitchEventsubSubscriptionType = "Twitch-Eventsub-Subscription-Type";
        public const string TwitchEventsubSubscriptionVersion = "Twitch-Eventsub-Subscription-Version";
    }

    public static class NotificationMessageTypes
    {
        public const string Verification = "webhook_callback_verification";
        public const string Notification = "notification";
        public const string Revocation = "revocation";
    }

    public static class NotificationStatuses
    {
        public const string Enabled = "enabled";
        public const string UserRemoved = "user_removed";
        public const string AuthorizationRevoked = "authorization_revoked";
        public const string NotificationFailuresExceeded = "notification_failures_exceeded";
    }

    public const string HmacPrefix = "sha256=";
}
