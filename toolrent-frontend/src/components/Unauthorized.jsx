import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

export default function Unauthorized() {
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>No autorizado</Typography>
      <Typography>Tu usuario no tiene permisos para acceder a esta sección.</Typography>
    </Box>
  );
}
