namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;

public abstract class Message : IMessage
{
    public static IMessage Decode(string message)
    {
        if (PrivMsg.Matches(message) is GroupCollection groupsPrivMsg)
        {
            return new PrivMsg(groupsPrivMsg);
        }

        if (Join.Matches(message) is GroupCollection groupsJoin)
        {
            return new Join(groupsJoin);
        }

        if (Part.Matches(message) is GroupCollection groupsPart)
        {
            return new Part(groupsPart);
        }

        if (Ping.Matches(message) is GroupCollection groupsPing)
        {
            return new Ping(groupsPing);
        }

        if (UserNotice.Matches(message) is GroupCollection groupsUserNotice)
        {
            return new UserNotice(groupsUserNotice);
        }

        if (ClearChat.Matches(message) is GroupCollection groupsClearChat)
        {
            return new ClearChat(groupsClearChat);
        }

        if (Notice.Matches(message) is GroupCollection groupsNotice)
        {
            return new Notice(groupsNotice);
        }

        if (ClearMsg.Matches(message) is GroupCollection groupsClearMsg)
        {
            return new ClearMsg(groupsClearMsg);
        }

        if (HostTargetStart.Matches(message) is GroupCollection groupsHostTargetStart)
        {
            return new HostTargetStart(groupsHostTargetStart);
        }

        if (HostTargetEnd.Matches(message) is GroupCollection groupsHostTargetEnd)
        {
            return new HostTargetEnd(groupsHostTargetEnd);
        }

        if (RoomState.Matches(message) is GroupCollection groupsRoomState)
        {
            return new RoomState(groupsRoomState);
        }

        if (UserState.Matches(message) is GroupCollection groupsUserState)
        {
            return new UserState(groupsUserState);
        }

        if (GlobalUserState.Matches(message) is GroupCollection groupsGlobalUserState)
        {
            return new GlobalUserState(groupsGlobalUserState);
        }

        if (Reconnect.Matches(message) is GroupCollection groupsReconnect)
        {
            return new Reconnect(groupsReconnect);
        }

        if (UnknownCommand.Matches(message) is GroupCollection groupsUnknownCommand)
        {
            return new UnknownCommand(groupsUnknownCommand);
        }

        if (NamesList.Matches(message) is GroupCollection groupsNamesList)
        {
            return new NamesList(groupsNamesList);
        }

        if (EndOfNamesList.Matches(message) is GroupCollection groupsEndOfNamesList)
        {
            return new EndOfNamesList(groupsEndOfNamesList);
        }

        if (Welcome.Matches(message) is GroupCollection groupsWelcome)
        {
            return new Welcome(groupsWelcome);
        }

        if (CapabilityAcknowledge.Matches(message) is GroupCollection groupsCapAck)
        {
            return new CapabilityAcknowledge(groupsCapAck);
        }

        return new Unknown(message);
    }

    public Message(GroupCollection groups)
    {
        if (groups is null)
        {
            throw new ArgumentNullException(nameof(groups));
        }
    }
}
