import React from "react";
import Button from "@mui/material/Button";
import Head from "next/head";

const HomePage = () => {
  return (
    <React.Fragment>
      <Head>
        <title>ArchieMate</title>
      </Head>
      <h1>The home page</h1>
      <Button variant="contained">Test</Button>
    </React.Fragment>
  );
};

export default HomePage;
