package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.elements.NavLinks.*
import com.archimond7450.archiemate.App
import com.archimond7450.archiemate.elements.StyledStandardElements.{codeElement, h1Element, h2Element, h3Element, h4Element, pElement}
import com.archimond7450.archiemate.elements.VariableParamTable
import com.raquo.laminar.api.L.{*, given}

object DocsPage {
  def render(): HtmlElement = {
    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element("Docs")
      ),
      h2Element("Built-in variables"),
      pElement("In your created commands you can refer to any built-in variable using the following syntax"),
      codeElement("${var:par1=val,par2='long val' ; par3 = \"more\", flag}"),
      pElement("for maximum configuration or just"),
      codeElement("${var}"),
      pElement("for a simple usage."),
      pElement("For details, please, see documentation of each built-in variable."),
      h3Element("Time"),
      pElement("Returns the current time. The format and the time zone can be configured using the parameters."),
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
                href("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns"),
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
                href("https://en.wikipedia.org/wiki/List_of_tz_database_time_zones"),
                "table"
              ),
              " (TZ identifier column)."
            )
          )
        )
      ),
      h4Element("Examples"),
      pElement("For simplicity these examples assume the current date is 31st of December 2025 and the current time is 20:15:10 UTC."),
      pElement("To get the UTC time in the standard formatting (20:15:10) you can use the following:"),
      codeElement("${time}"),
      pElement("To get the time in the GMT-6 timezone in the standard formatting (14:15:10) you can use the following:"),
      codeElement("${time:zone=GMT-6}"),
      pElement("To get the time in Prague in the standard formatting (21:15:10 when CET is used, 22:15:10 when CEST is used) you can use the following:"),
      codeElement("${time:zone=Europe/Prague}"),
      pElement("To get the UTC date and time in the format \"YYYY-MM-DD HH:mm:ss\" (2025-12-31 20:15:10) you can use the following:"),
      codeElement("${time:format=\"YYYY-MM-DD HH:mm:ss\"}"),
      pElement("To get the date and time in Dubai in the format \"DD.MM hh:mm:ss a z\" (01.01. 12:15:10 AM GST) you can use the following:"),
      codeElement("${time:format=\"DD.MM. hh:mm:ss a z\",zone=Asia/Dubai}"),
      h3Element("Chatters"),
      pElement("Returns a list of chatters specified before a command call separated by predefined or configured separator."),
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
      pElement("For these examples let's assume a command !notify is set to each example and then called like so:"),
      codeElement("@user1 @user2 @user3 !notify"),
      pElement("To get the simple list of chatters separated by one space character (@user1 @user2 @user3) you can use the following:"),
      codeElement("${chatters}"),
      pElement("To get the list of chatters separated with space and comma (@user1, @user2, @user3) you can use the following:"),
      codeElement("${chatters:separator=\", \"}"),
      h3Element("Sender"),
      pElement("Returns the display name of the user who called the command including the '@' character (can be configured to omit)."),
      h4Element("Parameters"),
      VariableParamTable.element(
        Seq(
          VariableParamTable.Row(
            name = "notag",
            optional = true,
            defaultValue = "",
            description = span("When specified (any value) the display name is returned without the '@' character.")
          )
        )
      ),
      h4Element("Examples"),
      pElement("For these examples let's assume a user with display name \"Archie\" calls a command set to the example."),
      pElement("To get \"@Archie\" you can use the following:"),
      codeElement("${sender}"),
      pElement("To get \"Archie\" you can use the following:"),
      codeElement("${sender:notag}"),
      codeElement("${sender:notag=whatever}"),
      h3Element("Random"),
      pElement("Returns a random integer number between 0 and 100, both inclusive and configurable."),
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
          ),
        )
      ),
      h4Element("Examples"),
      pElement("To get a random number between 0 and 100 you can use the following:"),
      codeElement("${random}"),
      pElement("To get a random number between 0 and 1000 you can use the following:"),
      codeElement("${random:to=1000}"),
      pElement("To get a random number between 1 and 6 you can use the following:"),
      codeElement("${random:from=1,to=6}"),
      h2Element("Built-in Twitch commands"),
      h3Element("!command"),
      pElement("You can use this command to create a new channel command, or edit or delete an existing channel command. If you try to create a command when it already exists or edit or delete a command that doesn't exist, this command fails with an error. The specified command name doesn't need to start with an exclamation mark (\"!\") - this is implied."),
      h4Element("Creating a command"),
      pElement("To create a new command named \"!from\" which returns \"France\" you can use any of the following:"),
      codeElement("!command add !from France"),
      codeElement("!command create from France"),
      codeElement("!command new from France"),
      h4Element("Editing a command"),
      pElement("To edit the already existing \"!from\" command to newly return \"Austria\" you can use any of the following:"),
      codeElement("!command edit from Austria"),
      codeElement("!command change !from Austria"),
      codeElement("!command update from Austria"),
      h4Element("Deleting a command"),
      pElement("To delete the already existing \"!from\" command you can use any of the following:"),
      codeElement("!command delete !from"),
      codeElement("!command remove from"),
      h3Element("!commands"),
      pElement("This command prints a link where table of channel commands can be viewed (or even moderated if logged in). There are no parameters, hence the following usage:"),
      codeElement("!commands"),
      pElement("Any passed parameters to this command are ignored.")
    )
  }
}
