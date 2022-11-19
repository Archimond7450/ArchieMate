using System;
using Xunit;
using ArchieMate.TwitchIRC.UserTypes;
using FluentAssertions;
using System.Collections.Generic;

namespace ArchieMate.TwitchIRC.UnitTests;

public class UserTypesTests
{
    public static string[] StringUserTypes = { "", "admin", "global_mod", "staff" };
    public static Types[] EnumUserTypes = { Types.NormalUser, Types.TwitchAdmin, Types.GlobalModerator, Types.TwitchEmployee };

    [Fact]
    public void KnownUserTypesAreCorrectlyDecoded()
    {
        for (int i = 0; i < StringUserTypes.Length; i++)
        {
            // Arrange
            Types expectedUserType = EnumUserTypes[i];

            // Act
            Types result = Decoder.Decode(StringUserTypes[i]);

            // Assert
            result.Should().Be(expectedUserType);
        }
    }

    [Fact]
    public void UnknownUserTypeRaisesException()
    {
        // Arrange
        string unknownUserType = Guid.NewGuid().ToString();

        // Act
        Action action = () => Decoder.Decode(unknownUserType);

        // Assert
        action.Should().Throw<ArgumentException>().WithMessage($"Invalid user type {unknownUserType}");
    }
}