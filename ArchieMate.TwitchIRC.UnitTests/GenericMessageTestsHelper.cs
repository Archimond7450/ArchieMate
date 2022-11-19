using System;
using IncomingMessage = ArchieMate.TwitchIRC.Messages.Incoming.Message;
using System.Text.RegularExpressions;
using FluentAssertions;
using System.Reflection;

namespace ArchieMate.TwitchIRC.UnitTests;

public static class GenericMessageTestsHelper
{
    public static void TestNullArgumentException<T>() where T : IncomingMessage
    {
        // Arrange
        var matchesStaticMethod = typeof(T).GetMethod("Matches");
        var nullParameter = new object?[] { null };

        var constructor = typeof(T).GetConstructor(new Type[] { typeof(GroupCollection) });

        // Act
        var matchesCall = () => matchesStaticMethod?.Invoke(null, nullParameter);
        var constructorCall = () => constructor?.Invoke(nullParameter);

        // Assert
        matchesStaticMethod.Should().NotBeNull();
        constructor.Should().NotBeNull();

        matchesCall.Should()
            .Throw<TargetInvocationException>()
            .WithInnerException<ArgumentNullException>()
            .WithMessage("Value cannot be null. (Parameter 'input')");

        constructorCall.Should()
            .Throw<TargetInvocationException>()
            .WithInnerException<ArgumentNullException>()
            .WithMessage("Value cannot be null. (Parameter 'groups')");
    }

    public static void TestIncomingSimpleMessage<T>(string message) where T : IncomingMessage
    {
        // Arrange
        var matchesStaticMethod = typeof(T).GetMethod("Matches");
        var matchesStaticMethodParameter = new object[] { message };

        var constructor = typeof(T).GetConstructor(new Type[] { typeof(GroupCollection) });

        // Act
        var matchesCall = () => matchesStaticMethod?.Invoke(null, matchesStaticMethodParameter);

        // Assert
        matchesStaticMethod.Should().NotBeNull();
        constructor.Should().NotBeNull();

        matchesCall.Should().NotThrow();
        var groups = matchesCall();

        groups.Should()
            .NotBeNull()
            .And.BeOfType<GroupCollection>();

        if (groups is GroupCollection g)
        {
            var constructorParameter = new object[] { g };

            var constructorCall = () => constructor?.Invoke(constructorParameter);

            constructorCall.Should().NotThrow();

            var objMessage = constructorCall();
            objMessage.Should()
                .NotBeNull()
                .And.BeOfType<T>();
        }
    }

    public static void TestIncomingMessage<T>(string message, object expectedProps) where T : IncomingMessage
    {
        // Arrange
        var matchesStaticMethod = typeof(T).GetMethod("Matches");
        var matchesStaticMethodParameter = new object[] { message };

        matchesStaticMethod.Should().NotBeNull();

        // Act
        var groups = matchesStaticMethod?.Invoke(null, matchesStaticMethodParameter);

        groups.Should().NotBeNull().And.BeOfType<GroupCollection>();

        // Assert
        if (groups is GroupCollection g)
        {
            var constructor = typeof(T).GetConstructor(new System.Type[] { typeof(GroupCollection) });
            var constructorParameter = new object[] { g };

            constructor.Should().NotBeNull();

            var objMessage = constructor?.Invoke(constructorParameter);
            objMessage.Should()
                .NotBeNull()
                .And.BeOfType<T>()
                .And.BeEquivalentTo(
                    expectedProps,
                    AssertionOptions => AssertionOptions.ComparingByMembers<T>());
        }
    }
}

