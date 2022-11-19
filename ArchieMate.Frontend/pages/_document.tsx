import { Container } from "@mui/material";
import { Html, Head, Main, NextScript } from "next/document";

export default function Document() {
  return (
    <Html>
      <Head>
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"
        />
      </Head>
      <body>
        <Container sx={{ marginTop: 5 }}>
          <Main />
        </Container>
        <NextScript />
      </body>
    </Html>
  );
}
