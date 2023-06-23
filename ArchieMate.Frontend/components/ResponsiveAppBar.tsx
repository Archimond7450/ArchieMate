import React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import Menu from "@mui/material/Menu";
import MenuIcon from "@mui/icons-material/Menu";
import Container from "@mui/material/Container";
import Avatar from "@mui/material/Avatar";
import Button from "@mui/material/Button";
import Tooltip from "@mui/material/Tooltip";
import MenuItem from "@mui/material/MenuItem";
import AdbIcon from "@mui/icons-material/Adb";
import Link from "next/link";
import HomeIcon from "@mui/icons-material/Home";
import ArticleIcon from "@mui/icons-material/Article";
import DashboardIcon from "@mui/icons-material/Dashboard";
import LogoutIcon from "@mui/icons-material/Logout";

interface LinkType {
  icon: JSX.Element;
  text: string;
  href: string;
  cy: string;
}

const iconStyles = {
  position: "relative",
  marginRight: 1,
  top: "6px",
};

const pages: LinkType[] = [
  {
    icon: <HomeIcon data-cy="appbar-home-icon" sx={iconStyles} />,
    text: "Home",
    href: "/",
    cy: "appbar-home",
  },
  {
    icon: <ArticleIcon data-cy="appbar-docs-icon" sx={iconStyles} />,
    text: "Docs",
    href: "/docs",
    cy: "appbar-docs",
  },
];
const settings: LinkType[] = [
  {
    icon: <DashboardIcon sx={iconStyles} />,
    text: "Dashboard",
    href: "/dashboard",
    cy: "appbar-dashboard",
  },
  {
    icon: <LogoutIcon sx={iconStyles} />,
    text: "Logout",
    href: "/logout",
    cy: "appbar-logout",
  },
];

const ResponsiveAppBar = () => {
  const [anchorElNav, setAnchorElNav] = React.useState<null | HTMLElement>(
    null
  );
  const [anchorElUser, setAnchorElUser] = React.useState<null | HTMLElement>(
    null
  );

  const handleOpenNavMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElNav(event.currentTarget);
  };
  const handleOpenUserMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElUser(event.currentTarget);
  };

  const handleCloseNavMenu = () => {
    setAnchorElNav(null);
  };

  const handleCloseUserMenu = () => {
    setAnchorElUser(null);
  };

  return (
    <React.Fragment>
      <AppBar position="fixed">
        <Container maxWidth="xl">
          <Toolbar disableGutters>
            <AdbIcon sx={{ display: { xs: "none", md: "flex" }, mr: 1 }} />
            <Link href="/" passHref data-cy="appbar-logo">
              <Typography
                variant="h6"
                noWrap
                component="a"
                sx={{
                  mr: 2,
                  display: { xs: "none", md: "flex" },
                  fontFamily: "monospace",
                  fontWeight: 700,
                  letterSpacing: ".3rem",
                  color: "inherit",
                  textDecoration: "none",
                }}
              >
                Archie|Mate
              </Typography>
            </Link>

            <Box sx={{ flexGrow: 1, display: { xs: "flex", md: "none" } }}>
              <IconButton
                size="large"
                aria-label="account of current user"
                aria-controls="menu-appbar"
                aria-haspopup="true"
                onClick={handleOpenNavMenu}
                color="inherit"
              >
                <MenuIcon />
              </IconButton>
              <Menu
                id="menu-appbar"
                anchorEl={anchorElNav}
                anchorOrigin={{
                  vertical: "bottom",
                  horizontal: "left",
                }}
                keepMounted
                transformOrigin={{
                  vertical: "top",
                  horizontal: "left",
                }}
                open={Boolean(anchorElNav)}
                onClose={handleCloseNavMenu}
                sx={{
                  display: { xs: "block", md: "none" },
                }}
              >
                {
                  // Map mobile friendly menu
                  pages.map((page) => (
                    <MenuItem key={page.href} onClick={handleCloseNavMenu}>
                      <Link href={page.href} passHref>
                        <Typography
                          component="a"
                          sx={{
                            my: 2,
                            color: "black",
                            display: "block",
                            width: "100%",
                            textDecoration: "none",
                          }}
                        >
                          {page.icon}
                          {page.text}
                        </Typography>
                      </Link>
                    </MenuItem>
                  ))
                }
              </Menu>
            </Box>
            <AdbIcon sx={{ display: { xs: "flex", md: "none" }, mr: 1 }} />
            <Link href="/" passHref data-cy="appbar-home">
              <Typography
                variant="h5"
                noWrap
                component="a"
                sx={{
                  mr: 2,
                  display: { xs: "flex", md: "none" },
                  flexGrow: 1,
                  fontFamily: "monospace",
                  fontWeight: 700,
                  letterSpacing: ".3rem",
                  color: "inherit",
                  textDecoration: "none",
                }}
              >
                Archie|Mate
              </Typography>
            </Link>
            <Box sx={{ flexGrow: 1, display: { xs: "none", md: "flex" } }}>
              {
                // Map normal menu
                pages.map((page) => (
                  <MenuItem key={page.href} onClick={handleCloseNavMenu}>
                    <Link href={page.href} passHref>
                      <Typography
                        component="a"
                        sx={{
                          my: 2,
                          color: "white",
                          display: "block",
                          width: "100%",
                          textDecoration: "none",
                        }}
                      >
                        {page.icon}
                        {page.text}
                      </Typography>
                    </Link>
                  </MenuItem>
                ))
              }
            </Box>

            <Box sx={{ flexGrow: 0 }}>
              <Tooltip title="Open settings">
                <IconButton onClick={handleOpenUserMenu} sx={{ p: 0 }}>
                  <Avatar alt="Remy Sharp" src="/static/images/avatar/2.jpg" />
                </IconButton>
              </Tooltip>
              <Menu
                sx={{ mt: "45px" }}
                id="menu-appbar"
                anchorEl={anchorElUser}
                anchorOrigin={{
                  vertical: "top",
                  horizontal: "right",
                }}
                keepMounted
                transformOrigin={{
                  vertical: "top",
                  horizontal: "right",
                }}
                open={Boolean(anchorElUser)}
                onClose={handleCloseUserMenu}
              >
                {settings.map((setting) => (
                  <MenuItem key={setting.href} onClick={handleCloseUserMenu}>
                    <Link href={setting.href} passHref>
                      <Typography
                        component="a"
                        sx={{
                          my: 2,
                          color: "black",
                          display: "block",
                          width: "100%",
                          textDecoration: "none",
                        }}
                      >
                        {setting.icon}
                        {setting.text}
                      </Typography>
                    </Link>
                  </MenuItem>
                ))}
              </Menu>
            </Box>
          </Toolbar>
        </Container>
      </AppBar>
      <Toolbar />
    </React.Fragment>
  );
};

export default ResponsiveAppBar;
