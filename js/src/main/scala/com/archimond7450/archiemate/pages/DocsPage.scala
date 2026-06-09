package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.elements.NavLinks.*
import com.archimond7450.archiemate.App
import com.archimond7450.archiemate.elements.StyledStandardElements.{
  codeElement,
  h1Element,
  h2Element,
  h3Element,
  h4Element,
  pElement
}
import com.archimond7450.archiemate.elements.VariableParamTable
import com.raquo.laminar.api.L.{*, given}

object DocsPage {
  def render(): HtmlElement = {
    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element("Docs")
      ),
      a(nameAttr("builtin-variables")),
      h2Element("Built-in variables"),
      pElement(
        "In your created commands you can refer to any built-in variable using the following syntax"
      ),
      codeElement("${var:par1=val,par2='long val' ; par3 = \"more\", flag}"),
      pElement("for maximum configuration or just"),
      codeElement("${var}"),
      pElement("for a simple usage."),
      pElement(
        "For details, please, see documentation of each built-in variable."
      ),
      a(nameAttr("builtin-variables-time")),
      h3Element("Time"),
      pElement(
        "Returns the current time. The format and the time zone can be configured using the parameters."
      ),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "format",
            optional = true,
            defaultValue = "HH:mm:ss",
            description = span(
              "Current time formatting. If an unsupported format is entered, an error is returned. For the exhaustive list of supported formats, please, see the following ",
              a(
                cls("font-bold"),
                target("blank"),
                href(
                  "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns"
                ),
                "table"
              ),
              "."
            )
          ),
          VariableParamTable.Row(
            name = "zone",
            optional = true,
            defaultValue = "UTC",
            description = span(
              "The time zone the current time will be converted to. If an unsupported time zone is entered, an error is returned. For an idea of supported time zone values, please, see the following ",
              a(
                cls("font-bold"),
                target("blank"),
                href(
                  "https://en.wikipedia.org/wiki/List_of_tz_database_time_zones"
                ),
                "table"
              ),
              " (TZ identifier column)."
            )
          )
        )
      ),
      h4Element("Examples"),
      pElement(
        "For simplicity these examples assume the current date is 31st of December 2025 and the current time is 20:15:10 UTC."
      ),
      pElement(
        "To get the UTC time in the standard formatting (20:15:10) you can use the following:"
      ),
      codeElement("${time}"),
      pElement(
        "To get the time in the GMT-6 timezone in the standard formatting (14:15:10) you can use the following:"
      ),
      codeElement("${time:zone=GMT-6}"),
      pElement(
        "To get the time in Prague in the standard formatting (21:15:10 when CET is used, 22:15:10 when CEST is used) you can use the following:"
      ),
      codeElement("${time:zone=Europe/Prague}"),
      pElement(
        "To get the UTC date and time in the format \"YYYY-MM-DD HH:mm:ss\" (2025-12-31 20:15:10) you can use the following:"
      ),
      codeElement("${time:format=\"YYYY-MM-DD HH:mm:ss\"}"),
      pElement(
        "To get the date and time in Dubai in the format \"DD.MM hh:mm:ss a z\" (01.01. 12:15:10 AM GST) you can use the following:"
      ),
      codeElement("${time:format=\"DD.MM. hh:mm:ss a z\",zone=Asia/Dubai}"),
      a(nameAttr("builtin-variables-chatters")),
      h3Element("Chatters"),
      pElement(
        "Returns a list of chatters specified before a command call separated by predefined or configured separator."
      ),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "separator",
            optional = true,
            defaultValue = "one space character",
            description = span(
              "Separator to be used between the chatters."
            )
          )
        )
      ),
      h4Element("Examples"),
      pElement(
        "For these examples let's assume a command !notify is set to each example and then called like so:"
      ),
      codeElement("@user1 @user2 @user3 !notify"),
      pElement(
        "To get the simple list of chatters separated by one space character (@user1 @user2 @user3) you can use the following:"
      ),
      codeElement("${chatters}"),
      pElement(
        "To get the list of chatters separated with space and comma (@user1, @user2, @user3) you can use the following:"
      ),
      codeElement("${chatters:separator=\", \"}"),
      a(nameAttr("builtin-variables-sender")),
      h3Element("Sender"),
      pElement(
        "Returns the display name of the user who called the command including the '@' character (can be configured to omit)."
      ),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "notag",
            optional = true,
            defaultValue = "",
            description = span(
              "When specified (any value) the display name is returned without the '@' character."
            )
          )
        )
      ),
      h4Element("Examples"),
      pElement(
        "For these examples let's assume a user with display name \"Archie\" calls a command set to the example."
      ),
      pElement("To get \"@Archie\" you can use the following:"),
      codeElement("${sender}"),
      pElement("To get \"Archie\" you can use the following:"),
      codeElement("${sender:notag}"),
      codeElement("${sender:notag=whatever}"),
      a(nameAttr("builtin-variables-random")),
      h3Element("Random"),
      pElement(
        "Returns a random integer number between 0 and 100, both inclusive and configurable."
      ),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "from",
            optional = true,
            defaultValue = "0",
            description = span("The lowest roll.")
          ),
          VariableParamTable.Row(
            name = "to",
            optional = true,
            defaultValue = "100",
            description = span("The highest roll.")
          )
        )
      ),
      h4Element("Examples"),
      pElement(
        "To get a random number between 0 and 100 you can use the following:"
      ),
      codeElement("${random}"),
      pElement(
        "To get a random number between 0 and 1000 you can use the following:"
      ),
      codeElement("${random:to=1000}"),
      pElement(
        "To get a random number between 1 and 6 you can use the following:"
      ),
      codeElement("${random:from=1,to=6}"),
      a(nameAttr("builtin-commands")),
      h2Element("Built-in commands"),
      a(nameAttr("builtin-commands-command")),
      h3Element("!command"),
      pElement(
        "You can use this command to create a new channel command, or edit or delete an existing channel command. If you try to create a command when it already exists or edit or delete a command that doesn't exist, this command fails with an error. The specified command name doesn't need to start with an exclamation mark (\"!\") - this is implied."
      ),
      h4Element("Creating a command"),
      pElement(
        "To create a new command named \"!from\" which returns \"France\" you can use any of the following:"
      ),
      codeElement("!command add !from France"),
      codeElement("!command create from France"),
      codeElement("!command new from France"),
      h4Element("Editing a command"),
      pElement(
        "To edit the already existing \"!from\" command to newly return \"Austria\" you can use any of the following:"
      ),
      codeElement("!command edit from Austria"),
      codeElement("!command change !from Austria"),
      codeElement("!command update from Austria"),
      h4Element("Deleting a command"),
      pElement(
        "To delete the already existing \"!from\" command you can use any of the following:"
      ),
      codeElement("!command delete !from"),
      codeElement("!command remove from"),
      a(nameAttr("builtin-commands-commands")),
      h3Element("!commands"),
      pElement(
        "This command prints a link where table of channel commands can be viewed (or even moderated if logged in). There are no parameters, hence the following usage:"
      ),
      codeElement("!commands"),
      pElement("Any passed parameters to this command are ignored."),
      a(nameAttr("builtin-commands-set")),
      h3Element("!set"),
      pElement("This command can be used to set variable to certain value."),
      codeElement(
        "!set [variable] [Any text here will be the value of the variable]"
      ),
      pElement("To set a variable foo to value bar you can use the following:"),
      codeElement("!set foo bar"),
      a(nameAttr("builtin-commands-unset")),
      h3Element("!unset"),
      pElement("This command can be used to unset one or more variables."),
      codeElement("!unset [variable1] [variable2] ..."),
      pElement("For example to unset variable foo, use the following:"),
      codeElement("!unset foo"),
      a(nameAttr("builtin-commands-title")),
      h3Element("!title"),
      pElement(
        "This command can be used without parameters to check the current title or for moderators or broadcaster to set a new title."
      ),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "newTitle",
            optional = true,
            defaultValue = "",
            description = span("The new title to set.")
          )
        )
      ),
      h4Element("Examples"),
      pElement("To get the current title you can use the following:"),
      codeElement("!title"),
      pElement(
        "To set the title to \"Trying out the Remake\" you can use the following:"
      ),
      codeElement("!title Trying out the Remake"),
      a(nameAttr("builtin-commands-game")),
      h3Element("!game"),
      pElement(
        "This command can be used without parameters to check the current game or for moderators or broadcaster to set a new game."
      ),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "newGame",
            optional = true,
            defaultValue = "",
            description = span("The new game to set.")
          )
        )
      ),
      h4Element("Examples"),
      pElement("To get the current game you can use the following:"),
      codeElement("!game"),
      pElement("To set the game to \"Gothic\" you can use the following:"),
      codeElement("!game Gothic"),
      a(nameAttr("builtin-commands-subs")),
      h3Element("!subs"),
      pElement(
        "This command can be used to check the number of subscribers. Any parameters are ignored."
      ),
      codeElement("!subs"),
      a(nameAttr("builtin-commands-uptime")),
      h3Element("!uptime"),
      pElement(
        "This command can be used to see how long a stream has been up for. Any parameters are ignored."
      ),
      codeElement("!uptime"),
      a(nameAttr("builtin-commands-followage")),
      h3Element("!followage"),
      pElement(
        "This command can be used to check whether you or someone follows the channel."
      ),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "user",
            optional = true,
            defaultValue = "",
            description = span(
              "Specify to check someone else's followage instead of yours. '@' prefix is ignored."
            )
          )
        )
      ),
      pElement(
        "To check whether you follow the channel or not use the following:"
      ),
      codeElement("!followage"),
      pElement(
        "To check whether user Wulf follows the channel or not use any of the following:"
      ),
      codeElement("!followage Wulf"),
      codeElement("!followage @Wulf"),
      a(nameAttr("builtin-commands-afk")),
      h3Element("!afk"),
      pElement(
        "This command can be used to indicate to the chatbot that you are going to be away. When you return and write anything to the chat, the chatbot will respond with any mentions you might have missed."
      ),
      codeElement("!afk"),
      a(nameAttr("builtin-commands-greets")),
      h3Element("!greets"),
      pElement(
        "This command can be used to configure automatic greets system. When enabled, the chatbot greets known people when they first join or chat in the channel."
      ),
      h4Element("Enabling automatic greets system"),
      pElement(
        "To enable automatic greets system, only the broadcaster may use the following:"
      ),
      codeElement("!greets on"),
      h4Element("Disabling automatic greets system"),
      pElement(
        "To disable automatic greets system, only the broadcaster may use the following:"
      ),
      codeElement("!greets off"),
      h4Element("Configuring automatic greets system mode"),
      pElement(
        "To change automatic greets system mode only the broadcaster can use the following command (newMode parameter case-insensitive):"
      ),
      codeElement("!greets mode [newMode]"),
      pElement(
        "To make the chatbot greet EVERYONE (probably undesirable in larger channels), use the following:"
      ),
      codeElement("!greets mode all"),
      pElement(
        "To make the chatbot greet only the channel moderators, use any of the following:"
      ),
      codeElement("!greets mode mods"),
      codeElement("!greets mode moderators"),
      pElement(
        "To make the chatbot greet only the channel moderators and VIPs, use any of the following:"
      ),
      codeElement("!greets mode modsvips"),
      codeElement("!greets mode moderatorsvips"),
      pElement(
        "To make the chatbot greet only the channel moderators, VIPs and subscribers, use any of the following:"
      ),
      codeElement("!greets mode modsvipssubs"),
      codeElement("!greets mode moderatorsvipssubscribers"),
      pElement(
        "To make the chatbot greet only the channel moderators, VIPs, subscribers and followers, use any of the following:"
      ),
      codeElement("!greets mode modsvipssubsfollows"),
      codeElement("!greets mode moderatorsvipssubscribersfollowers"),
      h4Element("Showing the list of configured greets"),
      pElement(
        "The chatbot will show the generic configured greets when the broadcaster uses the following command. The chatbot will show specific configured greets for any other user that calls the following command. This is the following usage:"
      ),
      codeElement("!greets show"),
      h4Element("Configuration of own nickname"),
      pElement(
        "Any user except the broadcaster may configure their own nickname the chatbot uses instead of standard mention. This is the following syntax:"
      ),
      codeElement("!greets name [yourName]"),
      pElement(
        s"For example if a user would use the following command, then on the assumption that there is only one standard greet configured - \"Hello $${user}\" and there is no specific greet configured, the chatbot would then greet the user with \"Hello Archie\":"
      ),
      codeElement("!greets name Archie"),
      h4Element("Creating new greets"),
      pElement(
        s"Broadcaster may create any number of generic greets. The same applies to other users and their specific greets. For example if the broadcaster wanted to add the generic greet \"Hi, there $${user}\" or anyone else wanted to add the same specific greet for themselves, they would use any of the following:"
      ),
      codeElement(s"!greets add Hi, there $${user}"),
      codeElement(s"!greets create Hi, there $${user}"),
      h4Element("Editing greets"),
      pElement(
        "Broadcaster and any user may also edit already created greets. There are multiple ways of editing greets."
      ),
      pElement(
        s"To edit the first greet to \"Hi $${user}\", you could use any of the following:"
      ),
      codeElement(s"!greets edit first Hi $${user}"),
      codeElement(s"!greets update 0 Hi $${user}"),
      codeElement(s"!greets change 1st Hi $${user}"),
      pElement(
        s"To edit the second greet to \"Welcome $${user}\", you could use any of the following:"
      ),
      codeElement(s"!greets edit 1 Welcome $${user}"),
      codeElement(s"!greets update 2nd Welcome $${user}"),
      codeElement(s"!greets change second Welcome $${user}"),
      pElement(
        s"To edit the third greet to \"Good to see you $${user}\", you could use any of the following:"
      ),
      codeElement(s"!greets edit 3rd Good to see you $${user}"),
      codeElement(s"!greets update third Good to see you $${user}"),
      codeElement(s"!greets change 2 Good to see you $${user}"),
      pElement(
        s"To edit the last greet to \"Hiya $${user}\", you could use the following:"
      ),
      codeElement(s"!greets edit last Hiya $${user}"),
      pElement(
        s"To edit fourth or later greet, only the shorthand syntax is available:"
      ),
      codeElement(s"!greets update 4 Warm gretings to you $${user}"),
      codeElement(s"!greets change 5th Warm greetings to you $${user}"),
      h4Element("Deleting greets"),
      pElement(
        "Broadcaster and any user may also delete already created greets. The syntax of deleting a greet is identical to editing them, except of course you specify no text (or if you specify it, it gets ignored):"
      ),
      codeElement(s"!greets delete 0"),
      codeElement(s"!greets remove third"),
      codeElement(s"!greets delete 4"),
      codeElement(s"!greets remove last"),
      h4Element("Resetting the system"),
      pElement(s"Broadcaster may reset the greets system with the following:"),
      codeElement(s"!greets reset"),
      h4Element("Testing greets"),
      pElement(
        "For convenience a user may use the following command to force the chatbot to \"test\" greet them. The chabot uses a random greet from the collection of standard and specific greets."
      ),
      codeElement("!greets test"),
      a(nameAttr("builtin-commands-poll")),
      h3Element("!poll"),
      pElement(
        "Broadcaster and channel moderators may use the following command to use the chatbot's poll system:"
      ),
      h4Element("Adding a poll to the chatbot's poll system memory"),
      pElement(
        "This command only adds a poll to the memory so that it can be reused. The poll then needs to be started manually. To create a poll with an alias \"like\", the question \"Do you like the stream?\", and the options \"Yes\", \"It's OK\" and \"No\" you would use one of the following:"
      ),
      codeElement(
        "!poll add like \"Do you like the stream?\" \"Yes\" \"It's OK\" \"No\""
      ),
      codeElement(
        "!poll create like \"Do you like the stream?\" \"Yes\" \"It's OK\" \"No\""
      ),
      h4Element("Adding another alias to a poll in the poll system memory"),
      pElement(
        "To add an alias \"feedback\" to already added poll with an alias \"like\" the following could be used:"
      ),
      codeElement("!poll alias like feedback"),
      h4Element("Editing poll in the poll system memory"),
      pElement(
        "To edit an existing poll with an alias \"like\" you could use one of the following:"
      ),
      codeElement(
        "!poll edit like \"Do you like the stream?\" \"Yes, very much!\" \"It's pretty good!\" \"It's OK.\" \"It could be better\" \"No\""
      ),
      codeElement(
        "!poll update like \"Do you like the stream?\" \"Yes, very much!\" \"It's pretty good!\" \"It's OK.\" \"It could be better\" \"No\""
      ),
      codeElement(
        "!poll change like \"Do you like the stream?\" \"Yes, very much!\" \"It's pretty good!\" \"It's OK.\" \"It could be better\" \"No\""
      ),
      h4Element("Deleting poll from the poll system memory"),
      pElement(
        "To delete an existing poll with an alias \"like\" you could use one of the following:"
      ),
      codeElement("!poll delete like"),
      codeElement("!poll remove like"),
      h4Element("Starting a quick poll without saving it to the chatbot"),
      pElement(
        "To quickly start a poll that ends in 60 seconds you could use the following:"
      ),
      codeElement(
        "!poll quick \"Do you like the stream?\" \"Yes, very much!\" \"It's pretty good!\" \"It's OK.\" \"It could be better\" \"No\" 60"
      ),
      h4Element("Starting a poll saved in the poll system memory"),
      pElement(
        "To reuse saved poll with alias \"feedback\" and start it, so that it ends in 120 seconds, you could use the following:"
      ),
      codeElement("!poll start feedback 120"),
      h4Element("Ending a started poll early"),
      pElement(
        "To end a started poll earlier you can use any of the following:"
      ),
      codeElement("!poll end"),
      codeElement("!poll terminate"),
      h4Element("Ending a started poll early and archiving it on Twitch"),
      pElement(
        "When ending the poll you can also make the chatbot archive the poll on Twitch so that the results are not accessible in the Twitch polls history. You can use the following:"
      ),
      codeElement("!poll archive"),
      a(nameAttr("builtin-commands-prediction")),
      h3Element("!prediction"),
      pElement(
        "Broadcaster and channel moderators may use the following command to use the chatbot's prediction system:"
      ),
      h4Element(
        "Adding a prediction to the chatbot's prediction system memory"
      ),
      pElement(
        "This command only adds a prediction to the memory so that it can be reused. The prediction then needs to be started manually. To create a prediction with an alias \"ltd\", the prediction title \"Who wins this Legion TD?\", and outcomes \"Team West\" and \"Team East\" you would use one of the following:"
      ),
      codeElement(
        "!prediction add ltd \"Who wins this Legion TD?\" \"Team West\" \"Team East\""
      ),
      codeElement(
        "!prediction create ltd \"Who wins this Legion TD?\" \"Team West\" \"Team East\""
      ),
      h4Element(
        "Adding another alias to a prediction in the prediction system memory"
      ),
      pElement(
        "To add an alias \"ltdend\" to already added prediction with an alias \"ltd\" the following could be used:"
      ),
      codeElement("!prediction alias ltd ltdend"),
      h4Element("Editing prediction in the prediction system memory"),
      pElement(
        "To edit an existing prediction with an alias \"ltd1v1\" you could use one of the following:"
      ),
      codeElement(
        "!poll edit ltd1v1 \"Do I win this LTD 1v1?\", \"Yes, on level 7 or earlier\", \"Yes, between level 8 - 10\", \"Yes, between level 11 - 17\", \"Yes, on level 18 or later\", \"No, on or before level 10\", \"No, between level 11 - 17\", \"No, on level 18 or later\""
      ),
      codeElement(
        "!poll update ltd1v1 \"Do I win this LTD 1v1?\", \"Yes, on level 7 or earlier\", \"Yes, between level 8 - 10\", \"Yes, between level 11 - 17\", \"Yes, on level 18 or later\", \"No, on or before level 10\", \"No, between level 11 - 17\", \"No, on level 18 or later\""
      ),
      codeElement(
        "!poll change ltd1v1 \"Do I win this LTD 1v1?\", \"Yes, on level 7 or earlier\", \"Yes, between level 8 - 10\", \"Yes, between level 11 - 17\", \"Yes, on level 18 or later\", \"No, on or before level 10\", \"No, between level 11 - 17\", \"No, on level 18 or later\""
      ),
      h4Element("Deleting prediction from the prediction system memory"),
      pElement(
        "To delete an existing prediction from the prediction system memory with an alias \"ltd1v1\" you could use one of the following:"
      ),
      codeElement("!poll delete ltd1v1"),
      codeElement("!poll remove ltd1v1"),
      h4Element("Starting a quick prediction without saving it to the chatbot"),
      pElement(
        "To quickly start a prediction that locks in 60 seconds you could use the following:"
      ),
      codeElement(
        "!prediction quick \"Will I win this 1v1\" \"Yes\" \"No\" 60"
      ),
      h4Element("Starting a prediction saved in the prediction system memory"),
      pElement(
        "To reuse saved prediction with alias \"ltd\" and start it, so that it locks in 120 seconds, you could use the following:"
      ),
      codeElement("!prediction start ltd 120"),
      h4Element("Cancelling a started or locked prediction"),
      pElement(
        "To cancel a started or locked prediction you can use the following:"
      ),
      codeElement("!prediction cancel"),
      h4Element("Locking a started prediction early"),
      pElement("To lock a started prediction early you can use the following:"),
      codeElement("!prediction lock"),
      h4Element("Resolving a started or locked prediction"),
      pElement(
        "To resolve a prediction with the first outcome you can use the following:"
      ),
      codeElement("!prediction resolve 0"),
      pElement(
        "To resolve a prediction with outcome that contains the string \"Yes\" you can use the following:"
      ),
      codeElement("!prediction resolve Yes"),
      pElement(
        "Make sure when resolving prediction with the outcome substring you use the unique substring, otherwise the wrong outcome may be used instead of the intended one!"
      )
    )
  }
}
