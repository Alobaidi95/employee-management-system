import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";
import { Box, Typography, Button } from "@mui/material";
import { colors } from "../theme/theme";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

// Error boundaries must be class components - React doesn't provide a
// hook equivalent (as of this writing). getDerivedStateFromError and
// componentDidCatch only exist on the class component API, and only a
// class component can intercept a render error thrown by its children
// and swap in a fallback UI instead of letting it crash the whole tree.
export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // In a real production setup this is where you'd send the error to a
    // logging service (Sentry, etc.) - for now, at least it's not silently
    // swallowed the way an uncaught render error otherwise would be.
    console.error("Uncaught error in component tree:", error, info);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: colors.ink,
          px: 2,
        }}
      >
        <Box sx={{ bgcolor: colors.surface, borderRadius: 3, p: 4, maxWidth: 400, textAlign: "center" }}>
          <Typography sx={{ fontWeight: 600, fontSize: 18, color: colors.textDark, mb: 1 }}>
            Something went wrong
          </Typography>
          <Typography sx={{ fontSize: 14, color: colors.textMuted, mb: 3 }}>
            An unexpected error occurred. Reloading the page usually fixes this.
          </Typography>
          <Button
            variant="contained"
            onClick={() => window.location.reload()}
            sx={{ bgcolor: colors.primary, textTransform: "none", fontWeight: 600, "&:hover": { bgcolor: colors.primaryHover } }}
          >
            Reload page
          </Button>
        </Box>
      </Box>
    );
  }
}
