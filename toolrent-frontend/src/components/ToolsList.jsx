// src/components/ToolsList.jsx
// ✅ VERSIÓN SIMPLIFICADA - Sin errores de imports

import { useEffect, useMemo, useState } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Grid from '@mui/material/Grid2'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Paper from '@mui/material/Paper'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogActions from '@mui/material/DialogActions'
import DeleteIcon from '@mui/icons-material/Delete'
import Tooltip from '@mui/material/Tooltip'
import CircularProgress from '@mui/material/CircularProgress'

import toolService from '../services/tool.service'

const emptyForm = {
  name: '',
  category: '',
  status: 'Disponible',
  replacementValue: '',
  stock: '',
}

const STATUS_OPTIONS = [
  'Disponible',
  'Prestada',
  'En uso',
  'En reparación',
  'Dada de baja',
]

export default function ToolsList() {
  const { keycloak, initialized } = useKeycloak()
  const [tools, setTools] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  
  const [openDialog, setOpenDialog] = useState(false)
  const [selectedTool, setSelectedTool] = useState(null)

  const isAuthenticated = useMemo(() => keycloak?.authenticated, [keycloak?.authenticated])
  const isAdmin = useMemo(() => 
    keycloak?.hasRealmRole?.('ADMIN') || keycloak?.hasRealmRole?.('admin'),
    [keycloak]
  )

  useEffect(() => {
    if (!initialized || !isAuthenticated) return
    loadTools()
  }, [initialized, isAuthenticated])

  const loadTools = async () => {
    try {
      setLoading(true)
      setError('')
      const rows = await toolService.getAll()
      setTools(rows)
    } catch (e) {
      console.error(e)
      setError('❌ No se pudieron cargar las herramientas.')
    } finally {
      setLoading(false)
    }
  }

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const colorFor = (status = '') => {
    const s = status.toLowerCase()
    if (s.includes('disponible')) return 'success'
    if (s.includes('uso')) return 'warning'
    if (s.includes('prestada')) return 'warning'
    if (s.includes('reparación')) return 'info'
    if (s.includes('baja')) return 'error'
    return 'default'
  }

  const formatCurrency = (value) => {
    if (!value) return '$0'
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0
    }).format(value)
  }

  const onCreate = async () => {
    setError('')
    setSuccessMsg('')

    // ✅ Validación de campos obligatorios
    if (!form.name || !form.category) {
      setError('⚠️ Completa Nombre y Categoría.')
      return
    }

    const replacementValue = Number(form.replacementValue || 0)
    const stock = Number(form.stock || 0)

    // ✅ Validación: Valor de reposición debe ser mayor a 0
    if (!form.replacementValue || replacementValue <= 0) {
      setError('⚠️ El Valor de Reposición debe ser mayor a 0.')
      return
    }

    // ✅ Validación: Stock debe ser mayor a 0
    if (!form.stock || stock <= 0) {
      setError('⚠️ El Stock debe ser mayor a 0.')
      return
    }

    if (replacementValue < 0) {
      setError('⚠️ Valor de reposición no puede ser negativo.')
      return
    }

    if (stock < 0) {
      setError('⚠️ Stock no puede ser negativo.')
      return
    }

    try {
      setLoading(true)
      await toolService.create({
        name: form.name,
        category: form.category,
        status: form.status,
        replacementValue,
        stock,
      })

      setForm({ ...emptyForm })
      setSuccessMsg('✅ Herramienta creada exitosamente')
      setTimeout(() => setSuccessMsg(''), 3000)
      await loadTools()
    } catch (e) {
      console.error(e)
      const backendMsg = e.response?.data?.message || ''
      setError(`❌ No se pudo crear. ${backendMsg}`)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenDialog = (tool) => {
    setSelectedTool(tool)
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setSelectedTool(null)
  }

  const handleConfirmDecommission = async () => {
    if (!selectedTool) return

    try {
      setLoading(true)
      setError('')
      setSuccessMsg('')
      
      await toolService.decommission(selectedTool.id)
      
      setSuccessMsg(`✅ Herramienta "${selectedTool.name}" dada de baja`)
      setTimeout(() => setSuccessMsg(''), 3000)
      handleCloseDialog()
      await loadTools()
    } catch (e) {
      console.error(e)
      const errorMsg = e.response?.data?.message || 
                      e.response?.data?.error || 
                      'No se pudo dar de baja.'
      setError(`❌ ${errorMsg}`)
      handleCloseDialog()
    } finally {
      setLoading(false)
    }
  }

  const canDecommission = (tool) => {
    if (!isAdmin) return false
    if (!tool) return false
    
    const status = tool.status?.toLowerCase() || ''
    if (status.includes('prestada')) return false
    if (status.includes('baja')) return false
    
    return true
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Gestión de Herramientas
      </Typography>

      {/* Formulario (solo Admin) */}
      {isAdmin && (
        <>
          <Typography variant="subtitle1" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            Crear Nueva Herramienta
          </Typography>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Nombre *"
                name="name"
                value={form.name}
                onChange={onChange}
                fullWidth
                size="small"
                disabled={loading}
                placeholder="Taladro Bosch"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Categoría *"
                name="category"
                value={form.category}
                onChange={onChange}
                fullWidth
                size="small"
                disabled={loading}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <TextField
                select
                label="Estado"
                name="status"
                value={form.status}
                onChange={onChange}
                fullWidth
                size="small"
                disabled={loading}
              >
                {STATUS_OPTIONS.map((option) => (
                  <MenuItem key={option} value={option}>
                    {option}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <TextField
                label="Valor Reposición *"
                name="replacementValue"
                value={form.replacementValue}
                onChange={onChange}
                fullWidth
                size="small"
                type="number"
                disabled={loading}
                required
                placeholder="50000"
                helperText="Obligatorio"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 1 }}>
              <TextField
                label="Stock *"
                name="stock"
                value={form.stock}
                onChange={onChange}
                fullWidth
                size="small"
                type="number"
                disabled={loading}
                required
                placeholder="1"
                helperText="Obligatorio"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 1 }}>
              <Button
                variant="contained"
                onClick={onCreate}
                disabled={loading}
                fullWidth
                sx={{ height: '40px' }}
              >
                {loading ? '...' : 'CREAR'}
              </Button>
            </Grid>
          </Grid>
        </>
      )}

      {/* Mensajes */}
      {error && (
        <Typography color="error" sx={{ mb: 2, bgcolor: '#ffebee', p: 2, borderRadius: 1 }}>
          {error}
        </Typography>
      )}
      {successMsg && (
        <Typography sx={{ mb: 2, bgcolor: '#e8f5e9', p: 2, borderRadius: 1, color: '#2e7d32' }}>
          {successMsg}
        </Typography>
      )}

      {/* Tabla */}
      <TableContainer component={Paper} sx={{ mt: 2 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: '#f5f5f5' }}>
              <TableCell sx={{ fontWeight: 'bold' }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Nombre</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Categoría</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Valor</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Stock</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
              {isAdmin && <TableCell align="center" sx={{ fontWeight: 'bold' }}>Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && tools.length === 0 ? (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 6} align="center">
                  <CircularProgress size={24} sx={{ mr: 1 }} />
                  Cargando...
                </TableCell>
              </TableRow>
            ) : tools.length === 0 ? (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 6} align="center">
                  No hay herramientas
                </TableCell>
              </TableRow>
            ) : (
              tools.map((tool) => (
                <TableRow key={tool.id ?? `${tool.name}-${tool.category}`} hover>
                  <TableCell>{tool.id}</TableCell>
                  <TableCell>{tool.name}</TableCell>
                  <TableCell>{tool.category}</TableCell>
                  <TableCell>{formatCurrency(tool.replacementValue)}</TableCell>
                  <TableCell>{tool.stock}</TableCell>
                  <TableCell>
                    <Chip
                      label={tool.status}
                      color={colorFor(tool.status)}
                      size="small"
                    />
                  </TableCell>
                  {isAdmin && (
                    <TableCell align="center">
                      {canDecommission(tool) ? (
                        <Tooltip title="Dar de baja">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() => handleOpenDialog(tool)}
                            disabled={loading}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                      ) : (
                        <Tooltip title={
                          tool.status?.toLowerCase().includes('prestada') 
                            ? 'No se puede (prestada)'
                            : 'Ya dada de baja'
                        }>
                          <span>
                            <IconButton color="error" size="small" disabled>
                              <DeleteIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Dialog confirmación */}
      <Dialog open={openDialog} onClose={handleCloseDialog}>
        <DialogTitle>Confirmar dar de baja</DialogTitle>
        <DialogContent>
          <DialogContentText>
            ¿Dar de baja <strong>"{selectedTool?.name}"</strong>?
            <br /><br />
            Esta acción:
            <ul>
              <li>Cambiará el estado a "Dada de baja"</li>
              <li>Establecerá el stock en 0</li>
            </ul>
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} disabled={loading}>
            Cancelar
          </Button>
          <Button 
            onClick={handleConfirmDecommission} 
            color="error" 
            variant="contained"
            disabled={loading}
          >
            {loading ? 'Dando de baja...' : 'Dar de baja'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}