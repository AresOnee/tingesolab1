// src/components/Sidemenu.jsx
import { useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import HomeIcon from '@mui/icons-material/Home';
import BuildIcon from '@mui/icons-material/Build';
import PeopleIcon from '@mui/icons-material/People';
import AssignmentIcon from '@mui/icons-material/Assignment';
import SettingsIcon from '@mui/icons-material/Settings';
import HistoryIcon from '@mui/icons-material/History';
import AssessmentIcon from '@mui/icons-material/Assessment'

export const drawerWidth = 240;

export default function Sidemenu() {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();

  // Verificar si el usuario tiene rol de ADMIN
  const isAdmin = keycloak?.hasRealmRole?.('ADMIN') || keycloak?.hasRealmRole?.('admin');
  const isUser = keycloak?.hasRealmRole?.('USER') || keycloak?.hasRealmRole?.('user');

  const menuItems = [
    { 
      text: 'Inicio', 
      icon: <HomeIcon />, 
      path: '/home',
      roles: ['USER', 'ADMIN']
    },
    { 
      text: 'Herramientas', 
      icon: <BuildIcon />, 
      path: '/tools',
      roles: ['USER','ADMIN']
    },
    { 
      text: 'Clientes', 
      icon: <PeopleIcon />, 
      path: '/clients',
      roles: ['ADMIN']
    },
    { 
      text: 'Préstamos', 
      icon: <AssignmentIcon />, 
      path: '/loans',
      roles: ['USER', 'ADMIN']
    },
    { 
      text: 'Kardex', 
      icon: <HistoryIcon />, 
      path: '/kardex',
      roles: ['USER', 'ADMIN']
    },
    {
      text: 'Reportes',
      icon: <AssessmentIcon />,
      path: '/reports',
      roles: ['USER', 'ADMIN']
    },
    { 
      text: 'Configuración', 
      icon: <SettingsIcon />, 
      path: '/config',
      roles: ['ADMIN']
    },
  ];

  // Filtrar elementos del menú según el rol del usuario
  const visibleItems = menuItems.filter(item => {
    if (isAdmin && item.roles.includes('ADMIN')) return true;
    if (isUser && item.roles.includes('USER')) return true;
    return false;
  });

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
        },
      }}
    >
      <Toolbar /> {/* espacio para la AppBar */}
      <List>
        {visibleItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton onClick={() => navigate(item.path)}>
              <ListItemIcon>
                {item.icon}
              </ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Drawer>
  );
}