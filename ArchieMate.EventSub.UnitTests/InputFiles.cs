using System.Reflection;

namespace ArchieMate.EventSub.UnitTests;

public static class InputFiles
{
    public static TextReader GetInputFile(string filename)
    {
        Assembly thisAssembly = Assembly.GetExecutingAssembly();

        var names = thisAssembly.GetManifestResourceNames();

        string path = "ArchieMate.EventSub.UnitTests.Data";

        return new StreamReader(thisAssembly.GetManifestResourceStream(path + "." + filename)!);
    }
}
