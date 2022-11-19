import { Container } from "@mui/material";

const Footer = () => {
  return (
    <Container
      sx={{
        marginTop: 5,
        textAlign: "center",
      }}
    >
      &copy;&nbsp;2022-{new Date().getFullYear()}&nbsp;
      <a
        target="_"
        href="https://twitch.tv/archimond7450"
        style={{ textDecoration: "none", color: "black", fontWeight: "bold" }}
      >
        Archimond7450
      </a>
    </Container>
  );
};

export default Footer;
