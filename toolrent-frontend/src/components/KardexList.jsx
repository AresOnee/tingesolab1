// src/components/KardexList.jsx
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
import MenuItem from '@mui/material/MenuItem'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'

import kardexService from '../services/kardex.service'

// Tipos de movimiento disponibles
const MOVEMENT_TYPES = [
  { value: '', label: 'Todos' },
  { value: 'INGRESO', label: 'Ingreso' },
  { value: 'PRESTAMO', label: 'Préstamo' },
  { value: 'DEVOLUCION', label: 'Devolución' },
  { value: 'BAJA', label: 'Baja' },
  { value: 'REPARACION', label: 'Reparación' },
]

// Función para formatear fechas
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('es-CL', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// Función para obtener el color del chip según el tipo de movimiento
const getMovementColor = (type) => {
  switch (type) {
    case 'INGRESO':
      return 'success'
    case 'PRESTAMO':
      return 'warning'
    case 'DEVOLUCION':
      return 'info'
    case 'BAJA':
      return 'error'
    case 'REPARACION':
      return 'default'
    default:
      return 'default'
  }
}

const getToolLabel = (movement) => {
  const tool = movement.tool || {}
  const toolId = tool.id ?? movement.toolId
  const toolName = tool.name ?? movement.toolName

  if (toolId && toolName) {
    return `#${toolId} - ${toolName}`
  }

  if (toolId) {
    return `#${toolId}`
  }

  if (toolName) {
    return toolName
  }

  return '-'
}

export default function KardexList() {
  const { keycloak, initialized } = useKeycloak()
  const [movements, setMovements] = useState([])
  const [filteredMovements, setFilteredMovements] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Filtros
  const [filterType, setFilterType] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [toolId, setToolId] = useState('')

  const isAuthenticated = useMemo(
    () => keycloak?.authenticated,
    [keycloak?.authenticated]
  )

  // Cargar todos los movimientos al inicio
  useEffect(() => {
    if (!initialized || !isAuthenticated) return
    loadAllMovements()
  }, [initialized, isAuthenticated])

  // Aplicar filtros cada vez que cambian los movimientos o los filtros
  useEffect(() => {
    applyFilters()
  }, [movements, filterType, startDate, endDate, toolId])

  const loadAllMovements = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await kardexService.getAllMovements()
      setMovements(data)
    } catch (e) {
      console.error(e)
      setError('No se pudieron cargar los movimientos del kardex.')
    } finally {
      setLoading(false)
    }
  }

  const applyFilters = () => {
    let filtered = [...movements]

    // Filtrar por tipo de movimiento
    if (filterType) {
      filtered = filtered.filter((m) => m.movementType === filterType)
    }

    // Filtrar por ID o nombre de herramienta
    if (toolId) {
      const searchTerm = toolId.toLowerCase()
      filtered = filtered.filter((m) => {
        const tool = m.tool || {}
        const toolIdentifier = tool.id ?? m.toolId
        const toolName = tool.name ?? m.toolName

        const idMatches =
          toolIdentifier != null && toolIdentifier.toString().includes(toolId)
        const nameMatches =
          typeof toolName === 'string' && toolName.toLowerCase().includes(searchTerm)

        return idMatches || nameMatches
      })
    }

    // Filtrar por rango de fechas
    if (startDate) {
      const start = new Date(startDate)
      filtered = filtered.filter((m) => new Date(m.movementDate) >= start)
    }
    if (endDate) {
      const end = new Date(endDate)
      end.setHours(23, 59, 59, 999) // Incluir todo el día final
      filtered = filtered.filter((m) => new Date(m.movementDate) <= end)
    }

    setFilteredMovements(filtered)
  }

  const handleSearchByDateRange = async () => {
    if (!startDate || !endDate) {
      setError('Debe ingresar fecha de inicio y fecha de fin')
      return
    }

    try {
      setLoading(true)
      setError('')
      const data = await kardexService.getMovementsByDateRange(startDate, endDate)
      setMovements(data)
    } catch (e) {
      console.error(e)
      setError('Error al buscar por rango de fechas')
    } finally {
      setLoading(false)
    }
  }

  const handleClearFilters = () => {
    setFilterType('')
    setStartDate('')
    setEndDate('')
    setToolId('')
    loadAllMovements()
  }

  if (!initialized || !isAuthenticated) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography>Cargando...</Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Kardex - Historial de Movimientos
      </Typography>

      {/* Sección de Filtros */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>
          Filtros
        </Typography>
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              select
              label="Tipo de Movimiento"
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              fullWidth
              size="small"
            >
              {MOVEMENT_TYPES.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              label="Buscar Herramienta (ID o Nombre)"
              value={toolId}
              onChange={(e) => setToolId(e.target.value)}
              fullWidth
              size="small"
              placeholder="Ej: Martillo o 1"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 2 }}>
            <TextField
              label="Fecha Inicio"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 2 }}>
            <TextField
              label="Fecha Fin"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 2 }}>
            <Button
              variant="outlined"
              onClick={handleClearFilters}
              fullWidth
              size="small"
              sx={{ height: '40px' }}
            >
              Limpiar Filtros
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {/* Mensaje de error */}
      {error && (
        <Typography color="error" sx={{ mb: 2 }}>
          {error}
        </Typography>
      )}

      {/* Indicador de carga */}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Tabla de Movimientos */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Herramienta</TableCell>
              <TableCell>Cantidad</TableCell>
              <TableCell>Usuario</TableCell>
              <TableCell>Fecha</TableCell>
              <TableCell>Observaciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredMovements.length === 0 && !loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No hay movimientos para mostrar
                </TableCell>
              </TableRow>
            ) : (
              filteredMovements.map((movement) => (
                <TableRow key={movement.id}>
                  <TableCell>{movement.id}</TableCell>
                  <TableCell>
                    <Chip
                      label={movement.movementType}
                      color={getMovementColor(movement.movementType)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{getToolLabel(movement)}</TableCell>
                  <TableCell>{movement.quantity}</TableCell>
                  <TableCell>{movement.username || '-'}</TableCell>
                  <TableCell>{formatDate(movement.movementDate)}</TableCell>
                  <TableCell>{movement.observations || '-'}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Resumen de movimientos filtrados */}
      <Box sx={{ mt: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Mostrando {filteredMovements.length} de {movements.length} movimientos
        </Typography>
      </Box>
    </Box>
  )
}
