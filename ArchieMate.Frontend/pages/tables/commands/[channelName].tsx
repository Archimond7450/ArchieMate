import React from "react";
import { useRouter } from "next/router";
import CommandsTable from "../../../components/CommandsTable";
import Head from "next/head";

const TableForChannelCommands = () => {
  const router = useRouter();
  const { channelName } = router.query;

  if (channelName && typeof channelName === "string") {
    return (
      <React.Fragment>
        <Head>
          <title>ArchieMate | {channelName}&apos;s commands</title>
        </Head>
        <h1>Commands for channel {channelName}</h1>
        <CommandsTable channelName={channelName} />
      </React.Fragment>
    );
  }

  return <React.Fragment></React.Fragment>;
};

export default TableForChannelCommands;
