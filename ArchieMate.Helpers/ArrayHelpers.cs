namespace ArchieMate.Helpers;
public static class ArrayHelpers
{
    public static bool ContainsString(string[] array, string value, StringComparison comparison = StringComparison.Ordinal)
    {
        return Array.FindAll(array, x => String.Equals(x, value, comparison)).Count() > 0;
    }
}
