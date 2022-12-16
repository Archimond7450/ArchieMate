import { Container, ScopedCssBaseline } from "@mui/material";
import { useRouter } from "next/router";
import React from "react";
import Footer from "./Footer";
import ResponsiveAppBar from "./ResponsiveAppBar";

type LayoutProps = {
  children: JSX.Element;
};

const Layout: React.FunctionComponent<LayoutProps> = ({
  children,
}: LayoutProps) => {
  const router = useRouter();

  if (router.asPath.startsWith("/widgets/")) {
    return <React.Fragment>{children}</React.Fragment>;
  }

  return (
    <ScopedCssBaseline>
      <div style={{ minHeight: "100%", minWidth: "100%" }}>
        <ResponsiveAppBar />
        <Container maxWidth="xl">{children}</Container>
        <Footer />
      </div>
    </ScopedCssBaseline>
  );
};

export default Layout;
