// src/layouts/MainLayout.jsx
import { useState, useMemo } from 'react'
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useKeycloak } from '@react-keycloak/web'

import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Toolbar from '@mui/material/Toolbar'
import IconButton from '@mui/material/IconButton'
import Typography from '@mui/material/Typography'
import Menu from '@mui/material/Menu'
import MenuIcon from '@mui/icons-material/Menu'
import Container from '@mui/material/Container'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import AccountCircle from '@mui/icons-material/AccountCircle'

export default function MainLayout() {
  const { keycloak, initialized } = useKeycloak()
  const navigate = useNavigate()

  const [anchorElNav, setAnchorElNav] = useState(null)
  const [anchorElUser, setAnchorElUser] = useState(null)

  // Determinar roles del usuario
  const isAdmin = useMemo(
    () => keycloak?.hasRealmRole?.('ADMIN') || keycloak?.hasRealmRole?.('admin'),
    [keycloak]
  )
  const isEmpleado = useMemo(
    () => keycloak?.hasRealmRole?.('EMPLEADO') || keycloak?.hasRealmRole?.('empleado'),
    [keycloak]
  )

  // Definir páginas según rol
  const pages = useMemo(() => {
    const allPages = []

    allPages.push({ label: 'Inicio', path: '/home' })

    if (isAdmin) {
      allPages.push({ label: 'Herramientas', path: '/tools' })
      allPages.push({ label: 'Clientes', path: '/clients' })
      allPages.push({ label: 'Tarifas', path: '/rates' })
    }

    if (isAdmin || isEmpleado) {
      allPages.push({ label: 'Préstamos', path: '/loans' })
      allPages.push({ label: 'Kardex', path: '/kardex' })
    }

    return allPages
  }, [isAdmin, isEmpleado])

  const handleOpenNavMenu = (event) => {
    setAnchorElNav(event.currentTarget)
  }

  const handleCloseNavMenu = () => {
    setAnchorElNav(null)
  }

  const handleOpenUserMenu = (event) => {
    setAnchorElUser(event.currentTarget)
  }

  const handleCloseUserMenu = () => {
    setAnchorElUser(null)
  }

  const handleLogout = () => {
    handleCloseUserMenu()
    keycloak?.logout()
  }

  const handleLogin = () => {
    keycloak?.login()
  }

  if (!initialized) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Typography>Cargando...</Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* AppBar */}
      <AppBar position="static">
        <Container maxWidth="xl">
          <Toolbar disableGutters>
            {/* Logo / Título Desktop */}
            <Typography
              variant="h6"
              noWrap
              component={Link}
              to="/home"
              sx={{
                mr: 2,
                display: { xs: 'none', md: 'flex' },
                fontWeight: 700,
                color: 'inherit',
                textDecoration: 'none',
              }}
            >
              🛠️ ToolRent
            </Typography>

            {/* Menú Mobile */}
            <Box sx={{ flexGrow: 1, display: { xs: 'flex', md: 'none' } }}>
              <IconButton
                size="large"
                onClick={handleOpenNavMenu}
                color="inherit"
              >
                <MenuIcon />
              </IconButton>
              <Menu
                anchorEl={anchorElNav}
                open={Boolean(anchorElNav)}
                onClose={handleCloseNavMenu}
                sx={{ display: { xs: 'block', md: 'none' } }}
              >
                {pages.map((page) => (
                  <MenuItem
                    key={page.path}
                    onClick={() => {
                      handleCloseNavMenu()
                      navigate(page.path)
                    }}
                  >
                    <Typography textAlign="center">{page.label}</Typography>
                  </MenuItem>
                ))}
              </Menu>
            </Box>

            {/* Logo / Título Mobile */}
            <Typography
              variant="h6"
              noWrap
              component={Link}
              to="/home"
              sx={{
                mr: 2,
                display: { xs: 'flex', md: 'none' },
                flexGrow: 1,
                fontWeight: 700,
                color: 'inherit',
                textDecoration: 'none',
              }}
            >
              🛠️ ToolRent
            </Typography>

            {/* Menú Desktop */}
            <Box sx={{ flexGrow: 1, display: { xs: 'none', md: 'flex' } }}>
              {pages.map((page) => (
                <Button
                  key={page.path}
                  onClick={() => navigate(page.path)}
                  sx={{ my: 2, color: 'white', display: 'block' }}
                >
                  {page.label}
                </Button>
              ))}
            </Box>

            {/* User Menu */}
            <Box sx={{ flexGrow: 0 }}>
              {keycloak?.authenticated ? (
                <>
                  <IconButton onClick={handleOpenUserMenu} color="inherit">
                    <AccountCircle />
                  </IconButton>
                  <Menu
                    anchorEl={anchorElUser}
                    open={Boolean(anchorElUser)}
                    onClose={handleCloseUserMenu}
                  >
                    <MenuItem disabled>
                      <Typography textAlign="center">
                        {keycloak?.tokenParsed?.preferred_username || 'Usuario'}
                      </Typography>
                    </MenuItem>
                    <MenuItem onClick={handleLogout}>
                      <Typography textAlign="center">Cerrar Sesión</Typography>
                    </MenuItem>
                  </Menu>
                </>
              ) : (
                <Button color="inherit" onClick={handleLogin}>
                  Iniciar Sesión
                </Button>
              )}
            </Box>
          </Toolbar>
        </Container>
      </AppBar>

      {/* Contenido Principal */}
      <Box component="main" sx={{ flexGrow: 1, bgcolor: 'background.default' }}>
        <Outlet />
      </Box>

      {/* Footer */}
      <Box
        component="footer"
        sx={{
          py: 2,
          px: 2,
          mt: 'auto',
          backgroundColor: (theme) => theme.palette.grey[200],
        }}
      >
        <Container maxWidth="xl">
          <Typography variant="body2" color="text.secondary" align="center">
            ToolRent © {new Date().getFullYear()} - Sistema de Gestión de Préstamos
          </Typography>
        </Container>
      </Box>
    </Box>
  )
}
