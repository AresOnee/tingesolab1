// src/components/ConfigManagement.jsx
import { useEffect, useMemo, useState } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import TextField from '@mui/material/TextField'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Paper from '@mui/material/Paper'
import IconButton from '@mui/material/IconButton'
import EditIcon from '@mui/icons-material/Edit'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/Cancel'
import Tooltip from '@mui/material/Tooltip'
import Chip from '@mui/material/Chip'
import Alert from '@mui/material/Alert'

import configService from '../services/config.service'

export default function ConfigManagement() {
  const { keycloak, initialized } = useKeycloak()
  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  
  // Estado para edición
  const [editingId, setEditingId] = useState(null)
  const [editValue, setEditValue] = useState('')

  const isAuthenticated = useMemo(() => keycloak?.authenticated, [keycloak?.authenticated])
  const isAdmin = useMemo(() => 
    keycloak?.hasRealmRole?.('ADMIN') || keycloak?.hasRealmRole?.('admin'),
    [keycloak]
  )

  // Cargar configuraciones
  useEffect(() => {
    if (!initialized || !isAuthenticated) return
    loadConfigs()
  }, [initialized, isAuthenticated])

  const loadConfigs = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await configService.getAll()
      setConfigs(data)
    } catch (e) {
      console.error(e)
      setError('No se pudieron cargar las configuraciones.')
    } finally {
      setLoading(false)
    }
  }

  const handleEdit = (config) => {
    setEditingId(config.id)
    setEditValue(config.configValue.toString())
    setSuccessMsg('')
    setError('')
  }

  const handleCancel = () => {
    setEditingId(null)
    setEditValue('')
  }

  const handleSave = async (config) => {
    try {
      setLoading(true)
      setError('')
      setSuccessMsg('')

      const newValue = parseFloat(editValue)

      if (isNaN(newValue) || newValue < 0) {
        setError('El valor debe ser un número mayor o igual a 0')
        return
      }

      await configService.updateById(config.id, newValue)

      setSuccessMsg(`Configuración "${getConfigLabel(config.configKey)}" actualizada exitosamente`)
      setEditingId(null)
      setEditValue('')
      await loadConfigs()
    } catch (e) {
      console.error(e)
      const errorMsg = e.response?.data?.message || 
                      e.response?.data?.error || 
                      'No se pudo actualizar la configuración.'
      setError(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(value)
  }

  const getConfigLabel = (key) => {
    switch(key) {
      case 'TARIFA_ARRIENDO_DIARIA':
        return 'Tarifa Diaria de Arriendo'
      case 'TARIFA_MULTA_DIARIA':
        return 'Tarifa Diaria de Multa'
      case 'CARGO_REPARACION':  // ✅ NUEVO
        return 'Cargo por Reparación'
      default:
        return key
    }
  }

  const getConfigDescription = (key) => {
    switch(key) {
      case 'TARIFA_ARRIENDO_DIARIA':
        return 'Costo diario por arrendar una herramienta'
      case 'TARIFA_MULTA_DIARIA':
        return 'Multa diaria por devolución atrasada'
      case 'CARGO_REPARACION':  // ✅ NUEVO
        return 'Cargo fijo por reparación de daños leves (Épica 2 - RN #16)'
      default:
        return 'Sin descripción'
    }
  }

  if (!isAuthenticated) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning">
          Debes iniciar sesión para ver las configuraciones.
        </Alert>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Gestión de Tarifas y Configuración
      </Typography>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {isAdmin 
          ? 'Como administrador, puedes editar las tarifas del sistema.'
          : 'Puedes consultar las tarifas actuales del sistema.'}
      </Typography>

      {/* Mensajes */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {successMsg && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccessMsg('')}>
          {successMsg}
        </Alert>
      )}

      {/* Tabla de configuraciones */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Configuración</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Descripción</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }} align="right">Valor</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Última Modificación</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Modificado Por</TableCell>
              {isAdmin && <TableCell sx={{ fontWeight: 'bold' }} align="center">Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {configs.map((config) => (
              <TableRow key={config.id}>
                <TableCell>
                  <Chip 
                    label={getConfigLabel(config.configKey)} 
                    color={config.configKey === 'CARGO_REPARACION' ? 'secondary' : 'primary'}  // ✅ Color diferente
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {config.description || getConfigDescription(config.configKey)}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  {editingId === config.id ? (
                    <TextField
                      value={editValue}
                      onChange={(e) => setEditValue(e.target.value)}
                      size="small"
                      type="number"
                      inputProps={{ min: 0 }}
                      sx={{ width: 150 }}
                      autoFocus
                    />
                  ) : (
                    <Typography variant="h6" color="primary">
                      {formatCurrency(config.configValue)}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  {new Date(config.lastModified).toLocaleString('es-CL')}
                </TableCell>
                <TableCell>
                  <Chip 
                    label={config.modifiedBy || 'N/A'} 
                    size="small" 
                    variant="outlined"
                  />
                </TableCell>
                {isAdmin && (
                  <TableCell align="center">
                    {editingId === config.id ? (
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        <Tooltip title="Guardar">
                          <IconButton
                            color="primary"
                            size="small"
                            onClick={() => handleSave(config)}
                            disabled={loading}
                          >
                            <SaveIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Cancelar">
                          <IconButton
                            color="default"
                            size="small"
                            onClick={handleCancel}
                            disabled={loading}
                          >
                            <CancelIcon />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    ) : (
                      <Tooltip title="Editar">
                        <IconButton
                          color="primary"
                          size="small"
                          onClick={() => handleEdit(config)}
                          disabled={loading}
                        >
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
            {!loading && configs.length === 0 && (
              <TableRow>
                <TableCell colSpan={isAdmin ? 6 : 5} align="center">
                  No hay configuraciones disponibles
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Información adicional */}
      <Box sx={{ mt: 3 }}>
        <Typography variant="body2" color="text.secondary">
          <strong>Nota:</strong> Las tarifas se expresan en valores diarios o fijos.
        </Typography>
        <Box component="ul" sx={{ mt: 1, pl: 2 }}>
          <li>
            <Typography variant="body2" color="text.secondary">
              <strong>Tarifa de arriendo:</strong> Se cobra por cada día que la herramienta está prestada.
            </Typography>
          </li>
          <li>
            <Typography variant="body2" color="text.secondary">
              <strong>Tarifa de multa:</strong> Se aplica por cada día de atraso en la devolución.
            </Typography>
          </li>
          <li>
            <Typography variant="body2" color="text.secondary">
              <strong>Cargo por reparación:</strong> Se aplica cuando una herramienta se devuelve con daños leves reparables (Épica 2 - RN #16).
            </Typography>
          </li>
        </Box>
      </Box>
    </Box>
  )
}