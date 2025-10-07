import Drawer from '@mui/material/Drawer'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import Toolbar from '@mui/material/Toolbar'
import { useNavigate } from 'react-router-dom'

export const drawerWidth = 240

const menu = [
  { text: 'Inicio', path: '/home' },
  { text: 'Herramientas', path: '/tools' },
  { text: 'Clientes', path: '/clients' },
  { text: 'Préstamos', path: '/loans' },
]

export default function Sidemenu() {
  const navigate = useNavigate()
  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' },
      }}
    >
      <Toolbar />
      <List>
        {menu.map(item => (
          <ListItem key={item.path} disablePadding>
            <ListItemButton onClick={() => navigate(item.path)}>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Drawer>
  )
}
