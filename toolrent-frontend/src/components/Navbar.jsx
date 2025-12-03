import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import { useKeycloak } from '@react-keycloak/web'
import { drawerWidth } from './Sidemenu'

export default function Navbar() {
  const { keycloak } = useKeycloak()

  return (
    <AppBar
      position="fixed"
      sx={{
        width: { sm: `calc(100% - ${drawerWidth}px)` },
        ml: { sm: `${drawerWidth}px` },
      }}
    >
      <Toolbar>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          Sistema de Gestión de Herramientas Toolrent
        </Typography>
        <Typography sx={{ mr: 2 }}>{keycloak?.tokenParsed?.preferred_username}</Typography>
        <Button color="inherit" onClick={() => keycloak.logout({ redirectUri: window.location.origin })}>
          LOGOUT
        </Button>
      </Toolbar>
    </AppBar>
  )
}
