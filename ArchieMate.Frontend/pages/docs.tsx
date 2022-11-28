import React from "react";
import Head from "next/head";
import Code from "../components/Code";

const DocsPage = () => {
  return (
    <React.Fragment>
      <Head>
        <title>ArchieMate | Docs</title>
      </Head>
      <h1>Docs</h1>
      <h2>Built-in variables</h2>
      <p>
        In your created commands you can refer to any built-in variable using
        this syntax
      </p>
      <Code>{"${variable:params}"}</Code>
      <p>for maximum configuration or just</p>
      <Code>{"${variable}"}</Code>
      <p>for a simple usage.</p>
      <p>For details please see each built-in variable</p>
      <h3>Time</h3>
      <p>Usage:</p>
      <Code>{"${time} | ${time:format=timeformat}"}</Code>
      <p>
        Returns the current time either using default format
        &quot;HH:mm:ss&quot; or specified using [timeformat].
      </p>
      <h2>Interaction on Twitch</h2>
      <h3>Managing commands for a channel</h3>
      <p>
        You can use the built-in command !command to create, edit or delete a
        command. For detailed usage please see subchapters. Here&apos;s the full
        usage:
      </p>
      <Code>
        !commands (add/create|edit/update/change|remove/delete) ([!]command)
        [response]
      </Code>
      <h4>Creating a command</h4>
      <p>
        To create a simple command on Twitch use the built-in command !command
        with the add or create parameter. The command must not exist yet. If it
        already exists, an error message is replied. Here are some examples:
      </p>
      <Code>!commands add !from France</Code>
      <p>
        This will create a command &quot;!from&quot; with a response
        &quot;France&quot;.
      </p>
      <Code>{"!commands create time It is ${time}"}</Code>
      <p>
        This will create a command &quot;!time&quot; with a response &quot;It is
        [evaluated value of built-in variable time]&quot;.
      </p>
      <h4>Editing a command</h4>
      <p>
        To edit a simple command on Twitch use the built-in command !command
        with the edit, update or change parameter. The command must already
        exist. If it doesn&apos;t, an error message is replied. Here is an
        example that changes the command &quot;!from&quot; so it will newly
        respond with &quot;Austria&quot;:
      </p>
      <Code>!commands update !from Austria</Code>
      <h4>Deleting a command</h4>
      <p>
        To delete a command, use the built-in command !command with the delete
        or remove parameter. The command must already exist. If it doesn&apos;t,
        an error message is replied. Here is an example that deletes the command
        &quot;!time&quot;:
      </p>
      <Code>!commands delete !time</Code>
    </React.Fragment>
  );
};

export default DocsPage;
