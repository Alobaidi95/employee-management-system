import { createTheme } from "@mui/material/styles";

// Design tokens for the whole app - the "employee ID badge" visual
// language established on the Login page, now shared everywhere so new
// pages stay consistent without re-declaring colors/fonts inline.
export const colors = {
  ink: "#12141C", // deep navy background
  surface: "#F7F7FA", // near-white card/panel surface
  primary: "#4A5FE8", // institutional indigo - primary actions
  primaryHover: "#3B4CD1",
  accent: "#E8B34A", // amber "lanyard" accent
  textDark: "#1B1E2B",
  textMuted: "#6B7089",
  textFaint: "#9599AC",
  border: "#E4E4EC",
};

const theme = createTheme({
  palette: {
    primary: {
      main: colors.primary,
    },
    background: {
      default: colors.ink,
      paper: colors.surface,
    },
    text: {
      primary: colors.textDark,
      secondary: colors.textMuted,
    },
  },
  typography: {
    fontFamily: '"IBM Plex Sans", sans-serif',
    h1: { fontFamily: '"IBM Plex Sans", sans-serif', fontWeight: 600 },
    h2: { fontFamily: '"IBM Plex Sans", sans-serif', fontWeight: 600 },
    h6: { fontFamily: '"IBM Plex Sans", sans-serif', fontWeight: 600 },
  },
  shape: {
    borderRadius: 10,
  },
});

export default theme;