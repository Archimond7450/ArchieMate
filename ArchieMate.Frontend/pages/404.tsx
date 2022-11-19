import React from "react";
import Head from "next/head";
import Grid from "@mui/material/Unstable_Grid2"; // Grid version 2

const NotFoundPage = () => {
  return (
    <React.Fragment>
      <Head>
        <title>ArchieMate | Not Found</title>
      </Head>
      <Grid container>
        <Grid
          container
          xs={6}
          sx={{
            paddingRight: 1,
            borderRight: "1px black solid",
          }}
          alignContent="center"
          justifyContent="flex-end"
        >
          <h1>Error 404</h1>
        </Grid>
        <Grid
          container
          xs={6}
          sx={{
            paddingLeft: 1,
          }}
          alignContent="center"
          justifyContent="flex-start"
        >
          The page was not found.
        </Grid>
      </Grid>
    </React.Fragment>
  );
};

export default NotFoundPage;
