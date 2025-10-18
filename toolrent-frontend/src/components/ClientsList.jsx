// src/components/ClientsList.jsx
// ✅ VERSIÓN SIMPLIFICADA - Sin errores de imports

import { useEffect, useMemo, useState } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import Grid from '@mui/material/Grid2'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Paper from '@mui/material/Paper'
import IconButton from '@mui/material/IconButton'
import EditIcon from '@mui/icons-material/Edit'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Chip from '@mui/material/Chip'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import CircularProgress from '@mui/material/CircularProgress'

import http from '../http-common'

const emptyForm = { name: '', rut: '', email: '', phone: '' }

/**
 * ✅ VERSIÓN SIMPLIFICADA - ClientsList
 * Sin imports problemáticos, funcionalidad completa
 */
export default function ClientsList() {
  const { keycloak, initialized } = useKeycloak()
  const [clients, setClients] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Estado para modal de edición
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editingClient, setEditingClient] = useState(null)
  const [editForm, setEditForm] = useState({ name: '', email: '', phone: '' })

  // Estado para cambio de estado
  const [stateDialogOpen, setStateDialogOpen] = useState(false)
  const [changingStateClient, setChangingStateClient] = useState(null)
  const [newState, setNewState] = useState('')

  const canSee = useMemo(() => keycloak?.authenticated, [keycloak?.authenticated])
  const isAdmin = useMemo(() => 
    keycloak?.hasRealmRole('admin') || keycloak?.hasRealmRole('ADMIN'), 
    [keycloak]
  )

  useEffect(() => {
    if (!initialized || !canSee) return
    fetchClients()
  }, [initialized, canSee])

  // ==================== FETCH CLIENTES ====================
  
  const fetchClients = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await http.get('/api/v1/clients/')
      setClients(response.data)
    } catch (e) {
      console.error(e)
      setError('No se pudieron cargar los clientes.')
    } finally {
      setLoading(false)
    }
  }

  // ==================== VALIDACIONES ====================

  const validateRUT = (rut) => {
    const rutPattern = /^\d{1,2}\.\d{3}\.\d{3}-[\dkK]$/
    return rutPattern.test(rut)
  }

  const validateEmail = (email) => {
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailPattern.test(email)
  }

  const validatePhone = (phone) => {
    const phonePattern = /^\+56\d{9}$/
    return phonePattern.test(phone)
  }

  // ==================== CREAR CLIENTE ====================

  const onChange = (e) => {
    const { name, value } = e.target
    setForm(f => ({ ...f, [name]: value }))
  }

  const onCreate = async () => {
    setError('')
    setSuccess('')

    if (!form.name || !form.rut || !form.email || !form.phone) {
      setError('⚠️ Completa todos los campos: Nombre, RUT, Email y Teléfono.')
      return
    }

    if (!validateRUT(form.rut)) {
      setError('⚠️ Formato de RUT inválido. Usa: 12.345.678-9')
      return
    }

    if (!validateEmail(form.email)) {
      setError('⚠️ Email inválido. Ejemplo: usuario@dominio.com')
      return
    }

    if (!validatePhone(form.phone)) {
      setError('⚠️ Teléfono inválido. Formato: +56912345678')
      return
    }

    try {
      setLoading(true)
      
      // ✅ Agregar estado "Activo" por defecto
      const clientData = {
        ...form,
        state: 'Activo'
      }
      
      await http.post('/api/v1/clients', clientData)
      setForm(emptyForm)
      await fetchClients()
      setSuccess('✅ Cliente creado exitosamente.')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      console.error(e)
      const status = e?.response?.status
      const backendMsg = e?.response?.data?.message || e?.response?.data?.error || ''
      
      if (status === 409) {
        setError(`❌ ${backendMsg}`)
      } else if (status === 403) {
        setError('❌ No tienes permisos para crear clientes.')
      } else if (status === 400) {
        setError(`❌ Datos inválidos: ${backendMsg}`)
      } else {
        setError('❌ No se pudo crear el cliente.')
      }
    } finally {
      setLoading(false)
    }
  }

  // ==================== EDITAR CLIENTE ====================

  const handleOpenEdit = (client) => {
    setEditingClient(client)
    setEditForm({
      name: client.name,
      email: client.email,
      phone: client.phone
    })
    setEditDialogOpen(true)
    setError('')
    setSuccess('')
  }

  const handleCloseEdit = () => {
    setEditDialogOpen(false)
    setEditingClient(null)
    setEditForm({ name: '', email: '', phone: '' })
    setError('')
  }

  const onEditChange = (e) => setEditForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSaveEdit = async () => {
    setError('')
    setSuccess('')

    if (!editForm.name || !editForm.email || !editForm.phone) {
      setError('⚠️ Completa todos los campos.')
      return
    }

    if (!validateEmail(editForm.email)) {
      setError('⚠️ Email inválido.')
      return
    }

    if (!validatePhone(editForm.phone)) {
      setError('⚠️ Teléfono inválido. Formato: +56912345678')
      return
    }

    try {
      setLoading(true)
      const updateData = {
        name: editForm.name,
        email: editForm.email,
        phone: editForm.phone,
        rut: editingClient.rut,
        state: editingClient.state
      }
      
      await http.put(`/api/v1/clients/${editingClient.id}`, updateData)
      await fetchClients()
      handleCloseEdit()
      setSuccess(`✅ Cliente "${editForm.name}" actualizado.`)
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      console.error(e)
      const status = e?.response?.status
      const backendMsg = e?.response?.data?.message || ''
      
      if (status === 409) {
        setError(`❌ ${backendMsg}`)
      } else if (status === 404) {
        setError('❌ Cliente no encontrado.')
      } else if (status === 400) {
        setError(`❌ Datos inválidos: ${backendMsg}`)
      } else {
        setError('❌ No se pudo actualizar el cliente.')
      }
    } finally {
      setLoading(false)
    }
  }

  // ==================== CAMBIAR ESTADO ====================

  const handleOpenChangeState = (client) => {
    setChangingStateClient(client)
    setNewState(client.state)
    setStateDialogOpen(true)
    setError('')
    setSuccess('')
  }

  const handleCloseChangeState = () => {
    setStateDialogOpen(false)
    setChangingStateClient(null)
    setNewState('')
    setError('')
  }

  const handleSaveState = async () => {
    setError('')

    if (!newState || newState === changingStateClient.state) {
      setError('⚠️ Selecciona un nuevo estado.')
      return
    }

    try {
      setLoading(true)
      const updateData = {
        name: changingStateClient.name,
        email: changingStateClient.email,
        phone: changingStateClient.phone,
        rut: changingStateClient.rut,
        state: newState
      }
      
      await http.put(`/api/v1/clients/${changingStateClient.id}`, updateData)
      await fetchClients()
      handleCloseChangeState()
      setSuccess(`✅ Estado cambiado a "${newState}".`)
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      const backendMsg = e?.response?.data?.message || ''
      setError(`❌ No se pudo cambiar el estado. ${backendMsg}`)
    } finally {
      setLoading(false)
    }
  }

  // ==================== RENDER ====================

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Gestión de Clientes
      </Typography>

      {/* Mensajes */}
      {error && (
        <Typography color="error" sx={{ mb: 2, bgcolor: '#ffebee', p: 2, borderRadius: 1 }}>
          {error}
        </Typography>
      )}
      {success && (
        <Typography sx={{ mb: 2, bgcolor: '#e8f5e9', p: 2, borderRadius: 1, color: '#2e7d32' }}>
          {success}
        </Typography>
      )}

      {/* Formulario de creación (Solo ADMIN) */}
      {isAdmin && (
        <>
          <Typography variant="subtitle1" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            Crear Nuevo Cliente
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Nombre"
                name="name"
                value={form.name}
                onChange={onChange}
                fullWidth
                placeholder="Juan Pérez"
                disabled={loading}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <TextField
                label="RUT"
                name="rut"
                value={form.rut}
                onChange={onChange}
                fullWidth
                placeholder="12.345.678-9"
                helperText="XX.XXX.XXX-X"
                disabled={loading}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Email"
                name="email"
                value={form.email}
                onChange={onChange}
                fullWidth
                placeholder="juan@toolrent.cl"
                disabled={loading}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <TextField
                label="Teléfono"
                name="phone"
                value={form.phone}
                onChange={onChange}
                fullWidth
                placeholder="+56912345678"
                helperText="+56XXXXXXXXX"
                disabled={loading}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }} display="flex" alignItems="center">
              <Button
                variant="contained"
                onClick={onCreate}
                disabled={loading}
                fullWidth
                sx={{ height: '56px' }}
              >
                {loading ? 'Creando...' : 'CREAR'}
              </Button>
            </Grid>
          </Grid>
        </>
      )}

      {/* Tabla de clientes */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: '#f5f5f5' }}>
              <TableCell sx={{ fontWeight: 'bold' }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Nombre</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>RUT</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Email</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Teléfono</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
              {isAdmin && <TableCell sx={{ fontWeight: 'bold' }}>Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && clients.length === 0 ? (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 6} align="center">
                  <CircularProgress size={24} sx={{ mr: 1 }} />
                  Cargando...
                </TableCell>
              </TableRow>
            ) : clients.length === 0 ? (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 6} align="center">
                  No hay clientes
                </TableCell>
              </TableRow>
            ) : (
              clients.map((client) => (
                <TableRow key={client.id} hover>
                  <TableCell>{client.id}</TableCell>
                  <TableCell>{client.name}</TableCell>
                  <TableCell>{client.rut}</TableCell>
                  <TableCell>{client.email}</TableCell>
                  <TableCell>{client.phone}</TableCell>
                  <TableCell>
                    <Chip
                      label={client.state}
                      color={client.state === 'Activo' ? 'success' : 'warning'}
                      size="small"
                      onClick={() => isAdmin && handleOpenChangeState(client)}
                      sx={{ cursor: isAdmin ? 'pointer' : 'default' }}
                    />
                  </TableCell>
                  {isAdmin && (
                    <TableCell>
                      <IconButton
                        color="primary"
                        onClick={() => handleOpenEdit(client)}
                        title="Editar cliente"
                        disabled={loading}
                      >
                        <EditIcon />
                      </IconButton>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Dialog editar */}
      <Dialog open={editDialogOpen} onClose={handleCloseEdit} maxWidth="sm" fullWidth>
        <DialogTitle>Editar Cliente</DialogTitle>
        <DialogContent>
          {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}
          <Box sx={{ mt: 2 }}>
            <TextField
              label="Nombre"
              name="name"
              value={editForm.name}
              onChange={onEditChange}
              fullWidth
              margin="normal"
              disabled={loading}
            />
            <TextField
              label="Email"
              name="email"
              value={editForm.email}
              onChange={onEditChange}
              fullWidth
              margin="normal"
              disabled={loading}
            />
            <TextField
              label="Teléfono"
              name="phone"
              value={editForm.phone}
              onChange={onEditChange}
              fullWidth
              margin="normal"
              helperText="+56912345678"
              disabled={loading}
            />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
              El RUT no se puede modificar.
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseEdit} disabled={loading}>
            Cancelar
          </Button>
          <Button onClick={handleSaveEdit} variant="contained" disabled={loading}>
            {loading ? 'Guardando...' : 'Guardar'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Dialog cambiar estado */}
      <Dialog open={stateDialogOpen} onClose={handleCloseChangeState} maxWidth="xs" fullWidth>
        <DialogTitle>Cambiar Estado del Cliente</DialogTitle>
        <DialogContent>
          {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}
          {changingStateClient && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body1" sx={{ mb: 2 }}>
                Cliente: <strong>{changingStateClient.name}</strong>
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                Estado actual: <Chip 
                  label={changingStateClient.state}
                  color={changingStateClient.state === 'Activo' ? 'success' : 'warning'}
                  size="small" 
                />
              </Typography>
              <FormControl fullWidth disabled={loading}>
                <InputLabel>Nuevo Estado</InputLabel>
                <Select
                  value={newState}
                  label="Nuevo Estado"
                  onChange={(e) => setNewState(e.target.value)}
                >
                  <MenuItem value="Activo">Activo</MenuItem>
                  <MenuItem value="Restringido">Restringido</MenuItem>
                </Select>
              </FormControl>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                <strong>Activo:</strong> Puede solicitar préstamos<br />
                <strong>Restringido:</strong> No puede solicitar préstamos
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseChangeState} disabled={loading}>
            Cancelar
          </Button>
          <Button onClick={handleSaveState} variant="contained" color="warning" disabled={loading}>
            {loading ? 'Cambiando...' : 'Cambiar Estado'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}