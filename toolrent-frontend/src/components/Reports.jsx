// src/components/Reports.jsx
import { useEffect, useState } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Paper from '@mui/material/Paper'
import Tabs from '@mui/material/Tabs'
import Tab from '@mui/material/Tab'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Alert from '@mui/material/Alert'

import reportService from '../services/report.service'

// Función para formatear fechas
const formatDate = (dateString) => {
  if (!dateString) return '-'
  try {
    return new Date(dateString).toLocaleDateString('es-CL')
  } catch {
    return '-'
  }
}

// Función para determinar el color del chip según el estado
const getStatusColor = (status) => {
  switch (status) {
    case 'Vigente':
      return 'success'
    case 'Atrasado':
      return 'error'
    case 'Devuelto':
      return 'default'
    default:
      return 'default'
  }
}

export default function Reports() {
  const { keycloak, initialized } = useKeycloak()
  const [currentTab, setCurrentTab] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Datos de los 3 reportes
  const [activeLoans, setActiveLoans] = useState([])
  const [clientsWithOverdues, setClientsWithOverdues] = useState([])
  const [mostLoanedTools, setMostLoanedTools] = useState([])

  // Filtros para préstamos activos
  const [loanFilters, setLoanFilters] = useState({
    startDate: '',
    endDate: '',
  })

  // Filtros para ranking de herramientas
  const [toolFilters, setToolFilters] = useState({
    startDate: '',
    endDate: '',
    limit: 10,
  })

  const isAuthenticated = keycloak?.authenticated

  // Cargar datos al cambiar de tab
  useEffect(() => {
    if (!initialized || !isAuthenticated) return

    switch (currentTab) {
      case 0:
        loadActiveLoans()
        break
      case 1:
        loadClientsWithOverdues()
        break
      case 2:
        loadMostLoanedTools()
        break
      default:
        break
    }
  }, [currentTab, initialized, isAuthenticated])

  // ========== RF6.1: PRÉSTAMOS ACTIVOS ==========
  const loadActiveLoans = async (filters = {}) => {
    try {
      setLoading(true)
      setError('')
      const data = await reportService.getActiveLoans(filters)
      setActiveLoans(data)
    } catch (e) {
      console.error(e)
      setError('Error al cargar préstamos activos')
    } finally {
      setLoading(false)
    }
  }

  const handleFilterActiveLoans = () => {
    const filters = {}
    if (loanFilters.startDate) filters.startDate = loanFilters.startDate
    if (loanFilters.endDate) filters.endDate = loanFilters.endDate
    loadActiveLoans(filters)
  }

  const handleClearLoanFilters = () => {
    setLoanFilters({ startDate: '', endDate: '' })
    loadActiveLoans()
  }

  // ========== RF6.2: CLIENTES CON ATRASOS ==========
  const loadClientsWithOverdues = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await reportService.getClientsWithOverdues()
      setClientsWithOverdues(data)
    } catch (e) {
      console.error(e)
      setError('Error al cargar clientes con atrasos')
    } finally {
      setLoading(false)
    }
  }

  // ========== RF6.3: HERRAMIENTAS MÁS PRESTADAS ==========
  const loadMostLoanedTools = async (filters = {}) => {
    try {
      setLoading(true)
      setError('')
      const data = await reportService.getMostLoanedTools(filters)
      setMostLoanedTools(data)
    } catch (e) {
      console.error(e)
      setError('Error al cargar ranking de herramientas')
    } finally {
      setLoading(false)
    }
  }

  const handleFilterMostLoaned = () => {
    const filters = { limit: toolFilters.limit }
    if (toolFilters.startDate) filters.startDate = toolFilters.startDate
    if (toolFilters.endDate) filters.endDate = toolFilters.endDate
    loadMostLoanedTools(filters)
  }

  const handleClearToolFilters = () => {
    setToolFilters({ startDate: '', endDate: '', limit: 10 })
    loadMostLoanedTools({ limit: 10 })
  }

  // ========== RENDER ==========
  if (!initialized || !isAuthenticated) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography>Cargando...</Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Reportes del Sistema
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Épica 6: Reportes y consultas para la toma de decisiones
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ width: '100%' }}>
        <Tabs
          value={currentTab}
          onChange={(e, newValue) => setCurrentTab(newValue)}
          variant="fullWidth"
        >
          <Tab label="Préstamos Activos (RF6.1)" />
          <Tab label="Clientes con Atrasos (RF6.2)" />
          <Tab label="Herramientas Más Prestadas (RF6.3)" />
        </Tabs>

        <Box sx={{ p: 3 }}>
          {/* ========== TAB 0: PRÉSTAMOS ACTIVOS ========== */}
          {currentTab === 0 && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Préstamos Activos (Vigentes y Atrasados)
              </Typography>

              {/* Filtros */}
              <Paper sx={{ p: 2, mb: 2, bgcolor: 'grey.50' }}>
                <Typography variant="subtitle2" gutterBottom>
                  Filtrar por fecha de préstamo
                </Typography>
                <Grid container spacing={2} alignItems="center">
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      label="Fecha Inicio"
                      type="date"
                      value={loanFilters.startDate}
                      onChange={(e) =>
                        setLoanFilters({ ...loanFilters, startDate: e.target.value })
                      }
                      fullWidth
                      size="small"
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      label="Fecha Fin"
                      type="date"
                      value={loanFilters.endDate}
                      onChange={(e) =>
                        setLoanFilters({ ...loanFilters, endDate: e.target.value })
                      }
                      fullWidth
                      size="small"
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 2 }}>
                    <Button
                      variant="contained"
                      onClick={handleFilterActiveLoans}
                      fullWidth
                      size="small"
                    >
                      Filtrar
                    </Button>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 2 }}>
                    <Button
                      variant="outlined"
                      onClick={handleClearLoanFilters}
                      fullWidth
                      size="small"
                    >
                      Limpiar
                    </Button>
                  </Grid>
                </Grid>
              </Paper>

              {/* Tabla de préstamos activos */}
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
                  <CircularProgress />
                </Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Cliente</TableCell>
                        <TableCell>Herramienta</TableCell>
                        <TableCell>Fecha Préstamo</TableCell>
                        <TableCell>Fecha Límite</TableCell>
                        <TableCell>Estado</TableCell>
                        <TableCell>Multa</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {activeLoans.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center">
                            No hay préstamos activos
                          </TableCell>
                        </TableRow>
                      ) : (
                        activeLoans.map((loan) => (
                          <TableRow key={loan.id}>
                            <TableCell>{loan.id}</TableCell>
                            <TableCell>{loan.client?.name || '-'}</TableCell>
                            <TableCell>{loan.tool?.name || '-'}</TableCell>
                            <TableCell>{formatDate(loan.startDate)}</TableCell>
                            <TableCell>{formatDate(loan.dueDate)}</TableCell>
                            <TableCell>
                              <Chip
                                label={loan.status}
                                color={getStatusColor(loan.status)}
                                size="small"
                              />
                            </TableCell>
                            <TableCell>${loan.fine?.toLocaleString() || 0}</TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                Total: {activeLoans.length} préstamos activos
              </Typography>
            </Box>
          )}

          {/* ========== TAB 1: CLIENTES CON ATRASOS ========== */}
          {currentTab === 1 && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Clientes con Préstamos Atrasados
              </Typography>

              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
                  <CircularProgress />
                </Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Nombre</TableCell>
                        <TableCell>RUT</TableCell>
                        <TableCell>Email</TableCell>
                        <TableCell>Teléfono</TableCell>
                        <TableCell>Estado</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {clientsWithOverdues.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={6} align="center">
                            No hay clientes con atrasos
                          </TableCell>
                        </TableRow>
                      ) : (
                        clientsWithOverdues.map((client) => (
                          <TableRow key={client.id}>
                            <TableCell>{client.id}</TableCell>
                            <TableCell>{client.name}</TableCell>
                            <TableCell>{client.rut}</TableCell>
                            <TableCell>{client.email}</TableCell>
                            <TableCell>{client.phone}</TableCell>
                            <TableCell>
                              <Chip
                                label={client.state}
                                color={client.state === 'Activo' ? 'success' : 'error'}
                                size="small"
                              />
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                Total: {clientsWithOverdues.length} clientes con atrasos
              </Typography>
            </Box>
          )}

          {/* ========== TAB 2: RANKING HERRAMIENTAS ========== */}
          {currentTab === 2 && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Ranking de Herramientas Más Prestadas
              </Typography>

              {/* Filtros */}
              <Paper sx={{ p: 2, mb: 2, bgcolor: 'grey.50' }}>
                <Typography variant="subtitle2" gutterBottom>
                  Filtrar por período
                </Typography>
                <Grid container spacing={2} alignItems="center">
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <TextField
                      label="Fecha Inicio"
                      type="date"
                      value={toolFilters.startDate}
                      onChange={(e) =>
                        setToolFilters({ ...toolFilters, startDate: e.target.value })
                      }
                      fullWidth
                      size="small"
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <TextField
                      label="Fecha Fin"
                      type="date"
                      value={toolFilters.endDate}
                      onChange={(e) =>
                        setToolFilters({ ...toolFilters, endDate: e.target.value })
                      }
                      fullWidth
                      size="small"
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 2 }}>
                    <TextField
                      label="Límite"
                      type="number"
                      value={toolFilters.limit}
                      onChange={(e) =>
                        setToolFilters({
                          ...toolFilters,
                          limit: parseInt(e.target.value) || 10,
                        })
                      }
                      fullWidth
                      size="small"
                      InputProps={{ inputProps: { min: 1, max: 50 } }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 2 }}>
                    <Button
                      variant="contained"
                      onClick={handleFilterMostLoaned}
                      fullWidth
                      size="small"
                    >
                      Filtrar
                    </Button>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 2 }}>
                    <Button
                      variant="outlined"
                      onClick={handleClearToolFilters}
                      fullWidth
                      size="small"
                    >
                      Limpiar
                    </Button>
                  </Grid>
                </Grid>
              </Paper>

              {/* Tabla de ranking */}
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
                  <CircularProgress />
                </Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Posición</TableCell>
                        <TableCell>ID Herramienta</TableCell>
                        <TableCell>Nombre</TableCell>
                        <TableCell align="right">Total Préstamos</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {mostLoanedTools.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={4} align="center">
                            No hay datos disponibles
                          </TableCell>
                        </TableRow>
                      ) : (
                        mostLoanedTools.map((tool, index) => (
                          <TableRow
                            key={tool.toolId}
                            sx={{
                              bgcolor: index < 3 ? 'action.hover' : 'inherit',
                            }}
                          >
                            <TableCell>
                              <Chip
                                label={`#${index + 1}`}
                                color={
                                  index === 0
                                    ? 'warning'
                                    : index === 1
                                    ? 'default'
                                    : index === 2
                                    ? 'info'
                                    : 'default'
                                }
                                size="small"
                              />
                            </TableCell>
                            <TableCell>{tool.toolId}</TableCell>
                            <TableCell>
                              <strong>{tool.toolName}</strong>
                            </TableCell>
                            <TableCell align="right">
                              <Chip
                                label={tool.loanCount}
                                color="primary"
                                size="small"
                              />
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                Mostrando top {mostLoanedTools.length} herramientas
              </Typography>
            </Box>
          )}
        </Box>
      </Paper>
    </Box>
  )
}
