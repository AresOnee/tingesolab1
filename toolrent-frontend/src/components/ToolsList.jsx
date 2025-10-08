// src/components/ToolsList.jsx
import { useEffect, useMemo, useState } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import TextField from '@mui/material/TextField'
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

import toolService from '../services/tool.service'

const emptyForm = {
  name: '',
  category: '',
  replacementValue: '',
  stock: '',
}

export default function ToolsList() {
  const { keycloak, initialized } = useKeycloak()
  const [tools, setTools] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  
  // Estado para el diálogo de confirmación
  const [openDialog, setOpenDialog] = useState(false)
  const [selectedTool, setSelectedTool] = useState(null)

  const isAuthenticated = useMemo(() => keycloak?.authenticated, [keycloak?.authenticated])
  const isAdmin = useMemo(() => 
    keycloak?.hasRealmRole?.('ADMIN') || keycloak?.hasRealmRole?.('admin'),
    [keycloak]
  )

  // Cargar herramientas
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
      setError('No se pudieron cargar las herramientas.')
    } finally {
      setLoading(false)
    }
  }

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const colorFor = (status = '') => {
    const s = status.toLowerCase()
    if (s.includes('disponible')) return 'success'
    if (s.includes('prestada')) return 'warning'
    if (s.includes('reparación')) return 'info'
    if (s.includes('baja')) return 'error'
    return 'default'
  }

  const onCreate = async () => {
    try {
      setLoading(true)
      setError('')
      setSuccessMsg('')
      
      await toolService.create({
        name: form.name,
        category: form.category,
        replacementValue: Number(form.replacementValue || 0),
        stock: Number(form.stock || 0),
      })
      
      setForm(emptyForm)
      setSuccessMsg('Herramienta creada exitosamente')
      await loadTools()
    } catch (e) {
      console.error(e)
      setError(e.response?.data?.message || 'No se pudo crear la herramienta.')
    } finally {
      setLoading(false)
    }
  }

  // Abrir diálogo de confirmación
  const handleOpenDialog = (tool) => {
    setSelectedTool(tool)
    setOpenDialog(true)
  }

  // Cerrar diálogo
  const handleCloseDialog = () => {
    setOpenDialog(false)
    setSelectedTool(null)
  }

  // Confirmar dar de baja
  const handleConfirmDecommission = async () => {
    if (!selectedTool) return

    try {
      setLoading(true)
      setError('')
      setSuccessMsg('')
      
      await toolService.decommission(selectedTool.id)
      
      setSuccessMsg(`Herramienta "${selectedTool.name}" dada de baja exitosamente`)
      handleCloseDialog()
      await loadTools()
    } catch (e) {
      console.error(e)
      const errorMsg = e.response?.data?.message || 
                      e.response?.data?.error || 
                      'No se pudo dar de baja la herramienta.'
      setError(errorMsg)
      handleCloseDialog()
    } finally {
      setLoading(false)
    }
  }

  // Verificar si se puede dar de baja una herramienta
  const canDecommission = (tool) => {
    if (!isAdmin) return false
    if (!tool) return false
    
    const status = tool.status?.toLowerCase() || ''
    
    // No se puede dar de baja si está prestada o ya está dada de baja
    if (status.includes('prestada')) return false
    if (status.includes('baja')) return false
    
    return true
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Herramientas
      </Typography>

      {/* Formulario de creación (solo Admin) */}
      {isAdmin && (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              label="Nombre"
              name="name"
              value={form.name}
              onChange={onChange}
              fullWidth
              size="small"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              label="Categoría"
              name="category"
              value={form.category}
              onChange={onChange}
              fullWidth
              size="small"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 2 }}>
            <TextField
              label="Valor Reposición"
              name="replacementValue"
              value={form.replacementValue}
              onChange={onChange}
              fullWidth
              size="small"
              type="number"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 2 }}>
            <TextField
              label="Stock"
              name="stock"
              value={form.stock}
              onChange={onChange}
              fullWidth
              size="small"
              type="number"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 2 }}>
            <Button
              variant="contained"
              onClick={onCreate}
              disabled={loading}
              fullWidth
            >
              CREAR
            </Button>
          </Grid>
        </Grid>
      )}

      {/* Mensajes */}
      {error && (
        <Typography color="error" sx={{ mb: 1 }}>
          {error}
        </Typography>
      )}
      {successMsg && (
        <Typography color="success.main" sx={{ mb: 1 }}>
          {successMsg}
        </Typography>
      )}

      {/* Tabla de herramientas */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>Categoría</TableCell>
              <TableCell>Valor Reposición</TableCell>
              <TableCell>Stock</TableCell>
              <TableCell>Estado</TableCell>
              {isAdmin && <TableCell align="center">Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {tools.map((tool) => (
              <TableRow key={tool.id ?? `${tool.name}-${tool.category}`}>
                <TableCell>{tool.id}</TableCell>
                <TableCell>{tool.name}</TableCell>
                <TableCell>{tool.category}</TableCell>
                <TableCell>${tool.replacementValue?.toLocaleString()}</TableCell>
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
                          ? 'No se puede dar de baja (prestada)'
                          : tool.status?.toLowerCase().includes('baja')
                          ? 'Ya está dada de baja'
                          : 'No disponible'
                      }>
                        <span>
                          <IconButton
                            color="error"
                            size="small"
                            disabled
                          >
                            <DeleteIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
            {!loading && tools.length === 0 && (
              <TableRow>
                <TableRow>
                  <TableCell colSpan={isAdmin ? 7 : 6} align="center">
                    Sin datos
                  </TableCell>
                </TableRow>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Diálogo de confirmación */}
      <Dialog
        open={openDialog}
        onClose={handleCloseDialog}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          Confirmar dar de baja
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            ¿Está seguro que desea dar de baja la herramienta "{selectedTool?.name}"?
            <br /><br />
            Esta acción:
            <ul>
              <li>Cambiará el estado a "Dada de baja"</li>
              <li>Establecerá el stock en 0</li>
              <li>No se podrá revertir fácilmente</li>
            </ul>
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} color="inherit">
            Cancelar
          </Button>
          <Button 
            onClick={handleConfirmDecommission} 
            color="error" 
            variant="contained"
            autoFocus
          >
            Dar de baja
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}