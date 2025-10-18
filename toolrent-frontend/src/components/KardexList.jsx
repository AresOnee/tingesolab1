// src/components/KardexList.jsx
// ✅ VERSIÓN ERP PROFESIONAL
// Estilo: Columnas separadas Entrada/Salida/Saldo como SAP, Oracle, etc.

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
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutline'

import kardexService from '../services/kardex.service'

const MOVEMENT_TYPES = [
  { value: '', label: 'Todos' },
  { value: 'REGISTRO', label: 'Registro' },
  { value: 'PRESTAMO', label: 'Préstamo' },
  { value: 'DEVOLUCION', label: 'Devolución' },
  { value: 'BAJA', label: 'Baja' },
  { value: 'REPARACION', label: 'Reparación' },
]

// Formatear fechas estilo ERP
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('es-CL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// Colores según tipo de movimiento (estilo ERP)
const getMovementColor = (type) => {
  switch (type) {
    case 'REGISTRO':
      return { bg: '#e8f5e9', text: '#2e7d32', label: 'Registro' }
    case 'PRESTAMO':
      return { bg: '#fff3e0', text: '#e65100', label: 'Préstamo' }
    case 'DEVOLUCION':
      return { bg: '#e3f2fd', text: '#1565c0', label: 'Devolución' }
    case 'BAJA':
      return { bg: '#ffebee', text: '#c62828', label: 'Baja' }
    case 'REPARACION':
      return { bg: '#f3e5f5', text: '#6a1b9a', label: 'Reparación' }
    default:
      return { bg: '#f5f5f5', text: '#616161', label: type }
  }
}

const getToolLabel = (movement) => {
  const tool = movement.tool || {}
  const toolId = tool.id ?? movement.toolId
  const toolName = tool.name ?? movement.toolName

  if (toolId && toolName) {
    return `#${toolId} - ${toolName}`
  }
  if (toolId) return `#${toolId}`
  if (toolName) return toolName
  return '-'
}

/**
 * ✅ KARDEX ESTILO ERP PROFESIONAL
 * 
 * Características:
 * - Columnas: Entrada | Salida | Saldo
 * - Saldo acumulado en tiempo real
 * - Colores según tipo de movimiento
 * - Iconos para entradas/salidas
 * - Diseño limpio y profesional
 */
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
  const [toolSearch, setToolSearch] = useState('')

  const isAuthenticated = useMemo(() => keycloak?.authenticated, [keycloak?.authenticated])

  useEffect(() => {
    if (!initialized || !isAuthenticated) return
    loadAllMovements()
  }, [initialized, isAuthenticated])

  useEffect(() => {
    applyFilters()
  }, [movements, filterType, startDate, endDate, toolSearch])

  const loadAllMovements = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await kardexService.getAllMovements()
      setMovements(data)
    } catch (e) {
      console.error(e)
      setError('No se pudieron cargar los movimientos.')
    } finally {
      setLoading(false)
    }
  }

  const applyFilters = () => {
    let filtered = [...movements]

    // Filtrar por tipo
    if (filterType) {
      filtered = filtered.filter((m) => m.movementType === filterType)
    }

    // Filtrar por herramienta
    if (toolSearch) {
      const searchTerm = toolSearch.toLowerCase()
      filtered = filtered.filter((m) => {
        const label = getToolLabel(m).toLowerCase()
        return label.includes(searchTerm)
      })
    }

    // Filtrar por fechas
    if (startDate) {
      const start = new Date(startDate)
      filtered = filtered.filter((m) => {
        const movDate = new Date(m.movementDate)
        return movDate >= start
      })
    }

    if (endDate) {
      const end = new Date(endDate)
      end.setHours(23, 59, 59)
      filtered = filtered.filter((m) => {
        const movDate = new Date(m.movementDate)
        return movDate <= end
      })
    }

    setFilteredMovements(filtered)
  }

  const handleClearFilters = () => {
    setFilterType('')
    setStartDate('')
    setEndDate('')
    setToolSearch('')
  }

  // ✅ CALCULAR SALDO ACUMULADO (estilo ERP)
  const calculateRunningBalance = () => {
    let balance = 0
    return filteredMovements.map((movement) => {
      balance += movement.quantity || 0
      return {
        ...movement,
        runningBalance: balance,
      }
    })
  }

  const movementsWithBalance = calculateRunningBalance()

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom sx={{ fontWeight: 600, color: '#1976d2' }}>
        📊 Kardex - Historial de Movimientos
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Sistema de control de inventario • Entrada/Salida/Saldo
      </Typography>

      {/* Filtros */}
      <Paper sx={{ p: 2, mb: 3, bgcolor: '#fafafa' }}>
        <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
          🔍 Filtros
        </Typography>
        <Grid container spacing={2}>
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
              label="Buscar Herramienta"
              value={toolSearch}
              onChange={(e) => setToolSearch(e.target.value)}
              fullWidth
              size="small"
              placeholder="ID o Nombre"
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
              sx={{ height: '40px' }}
            >
              Limpiar
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {/* Error */}
      {error && (
        <Typography color="error" sx={{ mb: 2, bgcolor: '#ffebee', p: 2, borderRadius: 1 }}>
          {error}
        </Typography>
      )}

      {/* Loading */}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Tabla estilo ERP */}
      <TableContainer component={Paper} sx={{ boxShadow: 3 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: '#1976d2' }}>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem' }}>ID</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem' }}>Fecha/Hora</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem' }}>Tipo</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem' }}>Herramienta</TableCell>
              <TableCell align="center" sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem', bgcolor: '#2e7d32' }}>
                📥 Entrada
              </TableCell>
              <TableCell align="center" sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem', bgcolor: '#c62828' }}>
                📤 Salida
              </TableCell>
              <TableCell align="center" sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem', bgcolor: '#1565c0' }}>
                💼 Saldo
              </TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem' }}>Usuario</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', fontSize: '0.9rem' }}>Observaciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {movementsWithBalance.length === 0 && !loading ? (
              <TableRow>
                <TableCell colSpan={9} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  No hay movimientos para mostrar
                </TableCell>
              </TableRow>
            ) : (
              movementsWithBalance.map((movement, index) => {
                const qty = movement.quantity || 0
                const isEntry = qty > 0
                const colors = getMovementColor(movement.movementType)

                return (
                  <TableRow 
                    key={movement.id}
                    sx={{ 
                      '&:hover': { bgcolor: '#f5f5f5' },
                      bgcolor: index % 2 === 0 ? 'white' : '#fafafa'
                    }}
                  >
                    {/* ID */}
                    <TableCell sx={{ fontWeight: 500 }}>{movement.id}</TableCell>

                    {/* Fecha/Hora */}
                    <TableCell sx={{ fontSize: '0.85rem' }}>
                      {formatDate(movement.movementDate)}
                    </TableCell>

                    {/* Tipo (con color) */}
                    <TableCell>
                      <Chip
                        label={colors.label}
                        size="small"
                        sx={{
                          bgcolor: colors.bg,
                          color: colors.text,
                          fontWeight: 600,
                          fontSize: '0.75rem',
                        }}
                      />
                    </TableCell>

                    {/* Herramienta */}
                    <TableCell sx={{ fontWeight: 500 }}>
                      {getToolLabel(movement)}
                    </TableCell>

                    {/* ENTRADA (verde) */}
                    <TableCell 
                      align="center" 
                      sx={{ 
                        bgcolor: isEntry ? '#e8f5e9' : 'transparent',
                        fontWeight: isEntry ? 700 : 400,
                        color: isEntry ? '#2e7d32' : '#bdbdbd',
                        fontSize: '1rem',
                      }}
                    >
                      {isEntry ? (
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5 }}>
                          <AddCircleOutlineIcon sx={{ fontSize: '1.2rem' }} />
                          {Math.abs(qty)}
                        </Box>
                      ) : (
                        '-'
                      )}
                    </TableCell>

                    {/* SALIDA (rojo) */}
                    <TableCell 
                      align="center" 
                      sx={{ 
                        bgcolor: !isEntry && qty !== 0 ? '#ffebee' : 'transparent',
                        fontWeight: !isEntry && qty !== 0 ? 700 : 400,
                        color: !isEntry && qty !== 0 ? '#c62828' : '#bdbdbd',
                        fontSize: '1rem',
                      }}
                    >
                      {!isEntry && qty !== 0 ? (
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5 }}>
                          <RemoveCircleOutlineIcon sx={{ fontSize: '1.2rem' }} />
                          {Math.abs(qty)}
                        </Box>
                      ) : (
                        '-'
                      )}
                    </TableCell>

                    {/* SALDO (azul) */}
                    <TableCell 
                      align="center" 
                      sx={{ 
                        bgcolor: '#e3f2fd',
                        fontWeight: 700,
                        color: movement.runningBalance >= 0 ? '#1565c0' : '#c62828',
                        fontSize: '1.1rem',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5 }}>
                        {movement.runningBalance >= 0 ? (
                          <TrendingUpIcon sx={{ fontSize: '1.2rem' }} />
                        ) : (
                          <TrendingDownIcon sx={{ fontSize: '1.2rem' }} />
                        )}
                        {movement.runningBalance}
                      </Box>
                    </TableCell>

                    {/* Usuario */}
                    <TableCell sx={{ fontSize: '0.85rem' }}>
                      {movement.username || '-'}
                    </TableCell>

                    {/* Observaciones */}
                    <TableCell sx={{ fontSize: '0.85rem', maxWidth: 200 }}>
                      {movement.observations || '-'}
                    </TableCell>
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Resumen */}
      <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Mostrando {filteredMovements.length} de {movements.length} movimientos
        </Typography>
        
        {movementsWithBalance.length > 0 && (
          <Paper sx={{ px: 3, py: 1, bgcolor: '#e3f2fd' }}>
            <Typography variant="body2" sx={{ fontWeight: 600, color: '#1565c0' }}>
              Saldo Final: {movementsWithBalance[movementsWithBalance.length - 1]?.runningBalance || 0} unidades
            </Typography>
          </Paper>
        )}
      </Box>
    </Box>
  )
}