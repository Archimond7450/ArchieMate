namespace ArchieMate.Chatbot;

internal static class Constants
{
    internal static class EnvironmentVariables
    {
        internal const string POSTGRES_PASSWORD = "POSTGRES_PASSWORD";
        internal const string POSTGRES_USER = "POSTGRES_USER";
        internal const string POSTGRES_DB = "POSTGRES_DB";
        internal const string POSTGRES_HOST = "POSTGRES_HOST";
    }

    internal const string DefaultPostgreSqlHost = "db";
    internal const int PostgreSqlPort = 5432;
}