package com.archimond7450.archiemate.twitch.irc

object TwitchExamples {
  object Incoming {
    object ClearChat {
      val ronniIsPermanentlyBannedFromDallasChannel = "@room-id=12345678;target-user-id=87654321;tmi-sent-ts=1642715756806 :tmi.twitch.tv CLEARCHAT #dallas :ronni"
      val allMessagesAreRemovedFromDallasChannel = "@room-id=12345678;tmi-sent-ts=1642715695392 :tmi.twitch.tv CLEARCHAT #dallas"
      val ronniIsTimedOutInDallasChannel = "@ban-duration=350;room-id=12345678;target-user-id=87654321;tmi-sent-ts=1642719320727 :tmi.twitch.tv CLEARCHAT #dallas :ronni"
    }
    object ClearMsg {
      val messageFromRonniWasRemovedFromDallasChannel = "@login=ronni;room-id=;target-msg-id=abc-123-def;tmi-sent-ts=1642720582342 :tmi.twitch.tv CLEARMSG #dallas :HeyGuys"
    }
    object GlobalUserState {
      val stateOfUserDallasAfterLoggingIn = "@badge-info=subscriber/8;badges=subscriber/6;color=#0D4200;display-name=dallas;emote-sets=0,33,50,237,793,2126,3517,4578,5569,9400,10337,12239;turbo=0;user-id=12345678;user-type=admin :tmi.twitch.tv GLOBALUSERSTATE"
    }
    object HostTarget {
      val abcChannelIsHostingXyzChannelWith10Viewers = ":tmi.twitch.tv HOSTTARGET #abc :xyz 10"
      val abcChannelIsNoLongerHostingAnyChannel = ":tmi.twitch.tv HOSTTARGET #abc :- 10"
    }
    object Join {
      val ronniJoinedDallasChannel = ":ronni!ronni@ronni.tmi.twitch.tv JOIN #dallas"
    }
    object Notice {
      val moderatorDeletedAMessage = "@msg-id=delete_message_success :tmi.twitch.tv NOTICE #bar :The message from foo is now deleted."
      val unableToSendWhisper = "@msg-id=whisper_restricted;target-user-id=12345678 :tmi.twitch.tv NOTICE #bar :Your settings prevent you from sending this whisper."
    }
    object Part {
      val ronniLeftDallasChannel = ":ronni!ronni@ronni.tmi.twitch.tv PART #dallas"
    }
    object PrivMsg {
      val ronniPostedInChannel = "@badge-info=;badges=turbo/1;color=#0D4200;display-name=ronni;emotes=25:0-4,12-16/1902:6-10;id=b34ccfc7-4977-403a-8a94-33c6bac34fb8;mod=0;room-id=1337;subscriber=0;tmi-sent-ts=1507246572675;turbo=1;user-id=1337;user-type=global_mod :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #ronni :Kappa Keepo Kappa"
      val ronniCheered100Bits = "@badge-info=;badges=staff/1,bits/1000;bits=100;color=;display-name=ronni;emotes=;id=b34ccfc7-4977-403a-8a94-33c6bac34fb8;mod=0;room-id=12345678;subscriber=0;tmi-sent-ts=1507246572675;turbo=1;user-id=12345678;user-type=staff :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #ronni :cheer100"
      val vipPostedInChannel = "@badge-info=;badges=vip/1,partner/1;client-nonce=cd15335a5e2059c3b087e22612de485e;color=;display-name=fun2bfun;emotes=;first-msg=0;flags=;id=1fd20412-965f-4c96-beb3-52266448f564;mod=0;returning-chatter=0;room-id=102336968;subscriber=0;tmi-sent-ts=1661372052425;turbo=0;user-id=12345678;user-type=;vip=1"
      val userSentHypeChatOf2USD = "@badge-info=;badges=glhf-pledge/1;color=;emotes=;first-msg=0;flags=;id=f6fb34f8-562f-4b4d-b628-32113d0ef4b0;mod=0;pinned-chat-paid-amount=200;pinned-chat-paid-canonical-amount=200;pinned-chat-paid-currency=USD;pinned-chat-paid-exponent=2;pinned-chat-paid-is-system-message=0;pinned-chat-paid-level=ONE;returning-chatter=0;room-id=12345678;subscriber=0;tmi-sent-ts=1687471984306;turbo=0;user-id=12345678;user-type="
      val userSentHypeChatOf1500KRW = "@badge-info=;badges=partner/1;color=;emotes=;first-msg=0;flags=;id=bf4a779b-e26f-4688-8dcc-c41f221d8bbe;mod=0;pinned-chat-paid-amount=1500;pinned-chat-paid-canonical-amount=1500;pinned-chat-paid-currency=KRW;pinned-chat-paid-exponent=0;pinned-chat-paid-is-system-message=0;pinned-chat-paid-level=ONE;returning-chatter=0;room-id=12345678;subscriber=0;tmi-sent-ts=1687474201232;turbo=0;user-id=12345678;user-type="
    }
  }
  object Outgoing {
    val replyToAnotherMessage = "@reply-parent-msg-id=b34ccfc7-4977-403a-8a94-33c6bac34fb8 PRIVMSG #ronni :Good idea!"
  }
}
