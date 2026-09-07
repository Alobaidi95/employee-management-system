import { Box, Typography, CircularProgress } from "@mui/material";
import { colors } from "../theme/theme";

interface StatCardProps {
  label: string;
  value: number | null; // null while loading
  accentColor?: string;
}

export default function StatCard({ label, value, accentColor = colors.primary }: StatCardProps) {
  return (
    <Box
      sx={{
        bgcolor: "#fff",
        border: `1px solid ${colors.border}`,
        borderRadius: 2,
        p: 2.5,
        minWidth: 150,
        flex: "1 1 150px",
      }}
    >
      <Typography
        sx={{
          fontFamily: '"IBM Plex Mono", monospace',
          fontSize: 10.5,
          letterSpacing: "0.1em",
          textTransform: "uppercase",
          color: colors.textFaint,
          mb: 1,
        }}
      >
        {label}
      </Typography>
      {value === null ? (
        <CircularProgress size={20} sx={{ color: accentColor }} />
      ) : (
        <Typography sx={{ fontSize: 28, fontWeight: 700, color: accentColor, fontFamily: '"IBM Plex Sans", sans-serif' }}>
          {value}
        </Typography>
      )}
    </Box>
  );
}
