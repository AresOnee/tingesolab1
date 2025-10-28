// src/components/Loans.jsx
import React, { useEffect, useMemo, useState } from "react";
import {
  Box,
  Typography,
  Button,
  TextField,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Grid,
  Card,
  CardContent,
  Tooltip,
  ListItemText,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  AssignmentReturn as ReturnIcon,
  CheckCircle as CheckIcon,
  Error as ErrorIcon,
  Inventory as InventoryIcon,
  CalendarMonth as CalendarIcon,
} from '@mui/icons-material';
import http from "../http-common";
import ReturnLoanModal from "./ReturnLoanModal";
import { useSnackbar } from "../contexts/SnackbarContext";

export default function Loans() {
  const { showSuccess, showError } = useSnackbar();

  const [clients, setClients] = useState([]);
  const [tools, setTools] = useState([]);
  const [loans, setLoans] = useState([]);
  const [form, setForm] = useState({ clientId: "", toolId: "", dueDate: "" });
  const [loading, setLoading] = useState(false);

  const [returnModalOpen, setReturnModalOpen] = useState(false);
  const [selectedLoan, setSelectedLoan] = useState(null);

  const clientsMap = useMemo(() => {
    const m = new Map();
    for (const c of clients) m.set(c.id, c);
    return m;
  }, [clients]);

  const toolsMap = useMemo(() => {
    const m = new Map();
    for (const t of tools) m.set(t.id, t);
    return m;
  }, [tools]);

  const getAvailableUnits = (toolId) => {
    const tool = toolsMap.get(Number(toolId));
    if (!tool) return 0;

    const activeLoans = loans.filter(
      l => (l.toolId || l.tool?.id || l.tool_id) === toolId && !l.returnDate
    ).length;

    return tool.stock - activeLoans;
  };

  const getAvailabilityColor = (available, stock) => {
    if (available === 0) return '#ef4444';
    if (available <= stock / 2) return '#f59e0b';
    return '#10b981';
  };

  const stats = useMemo(() => {
    const active = loans.filter(l => !l.returnDate).length;
    const overdue = loans.filter(l => l.status === "Atrasado" && !l.returnDate).length;
    const returned = loans.filter(l => l.returnDate).length;
    const totalFines = loans.reduce((sum, l) => sum + (l.fine || 0), 0);

    return { active, overdue, returned, totalFines };
  }, [loans]);

  const clientLabel = (loan) => {
    if (loan.client && typeof loan.client === 'object' && loan.client.name) {
      return `${loan.client.id} - ${loan.client.name}`;
    }
    const id = loan.clientId || loan.client_id || loan.client?.id;
    if (!id) return "—";
    const client = clientsMap.get(Number(id));
    return client ? `${client.name}` : `ID ${id}`;
  };

  const clientNameOnly = (loan) => {
    if (loan.client && typeof loan.client === 'object' && loan.client.name) {
      return loan.client.name;
    }
    const id = loan.clientId || loan.client_id || loan.client?.id;
    if (!id) return "Desconocido";
    const client = clientsMap.get(Number(id));
    return client ? client.name : "Desconocido";
  };

  const toolLabel = (loan) => {
    if (loan.tool && typeof loan.tool === 'object' && loan.tool.name) {
      return loan.tool.name;
    }
    const id = loan.toolId || loan.tool_id || loan.tool?.id;
    if (!id) return "—";
    const tool = toolsMap.get(Number(id));
    return tool ? tool.name : `ID ${id}`;
  };

  const toolNameOnly = (loan) => {
    if (loan.tool && typeof loan.tool === 'object' && loan.tool.name) {
      return loan.tool.name;
    }
    const id = loan.toolId || loan.tool_id || loan.tool?.id;
    if (!id) return "Desconocida";
    const tool = toolsMap.get(Number(id));
    return tool ? tool.name : "Desconocida";
  };

  const getLoanDate = (loan) => {
    return loan.loanDate || loan.loan_date || loan.startDate || "—";
  };

  const formatCurrency = (value) => {
    if (value == null || value === 0) return "$0";
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(value);
  };

  useEffect(() => {
    fetchAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function fetchAll() {
    try {
      setLoading(true);
      const [cRes, tRes, lRes] = await Promise.all([
        http.get("/api/v1/clients/"),
        http.get("/api/v1/tools/"),
        http.get("/api/v1/loans/"),
      ]);

      const clientsData = Array.isArray(cRes.data) ? cRes.data : (cRes.data?.content ?? []);
      const toolsData = Array.isArray(tRes.data) ? tRes.data : (tRes.data?.content ?? []);
      const loansData = Array.isArray(lRes.data) ? lRes.data : (lRes.data?.content ?? []);

      setClients(clientsData);
      setTools(toolsData);
      setLoans(loansData);
    } catch (err) {
      console.error(err);
      showError("No se pudieron cargar los datos");
    } finally {
      setLoading(false);
    }
  }

  function extractErrorMessage(error) {
    // ✅ Log para debugging
    console.log("Error completo:", error);
    console.log("Error response:", error.response);
    console.log("Error response data:", error.response?.data);

    if (!error.response) {
      return "Error de conexión con el servidor. Verifica tu conexión.";
    }

    const status = error.response.status;
    const data = error.response.data;

    // ✅ PRIORIDAD 1: Si el backend envía un string directamente
    if (typeof data === 'string') {
      // Si es HTML, intentar extraer el mensaje
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = data;
      const textContent = tempDiv.textContent || tempDiv.innerText || data;
      return textContent.trim() || "Error al procesar la solicitud.";
    }

    // ✅ PRIORIDAD 2: Objeto con propiedad 'message'
    if (data && typeof data === 'object') {
      // Intentar múltiples propiedades donde puede venir el mensaje
      const message = data.message || data.error || data.detail || data.title;
      
      if (message && message !== 'Bad Request' && message !== 'Internal Server Error') {
        return message;
      }

      // ✅ PRIORIDAD 3: Si tiene 'reason' (algunas APIs REST usan esto)
      if (data.reason) {
        return data.reason;
      }

      // ✅ PRIORIDAD 4: Si tiene 'errors' como array
      if (data.errors && Array.isArray(data.errors) && data.errors.length > 0) {
        return data.errors.map(e => e.message || e).join(', ');
      }
    }

    // ✅ Mensajes genéricos según código de estado
    const defaultMessages = {
      400: "Solicitud inválida. Verifica los datos ingresados.",
      401: "No autorizado. Por favor inicia sesión.",
      403: "No tienes permisos para realizar esta acción.",
      404: "Recurso no encontrado.",
      409: "Conflicto: La operación no se puede completar.",
      422: "Los datos enviados no son válidos.",
      500: "Error interno del servidor. Por favor contacta al administrador.",
      503: "Servicio no disponible. Intenta más tarde.",
    };

    return defaultMessages[status] || "Error desconocido. Por favor contacta al administrador.";
  }

  async function handleCreate(e) {
    e.preventDefault();
    
    if (!form.clientId || !form.toolId || !form.dueDate) {
      showError("Por favor completa todos los campos");
      return;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selectedDate = new Date(form.dueDate + 'T00:00:00');
    
    if (selectedDate < today) {
      showError("La fecha límite no puede ser anterior a hoy");
      return;
    }

    const selectedTool = toolsMap.get(Number(form.toolId));
    if (selectedTool) {
      if (selectedTool.stock < 1) {
        showError(`La herramienta "${selectedTool.name}" no tiene stock (Stock: ${selectedTool.stock})`);
        return;
      }

      const availableUnits = getAvailableUnits(selectedTool.id);
      const activeLoans = loans.filter(
        l => (l.toolId || l.tool?.id || l.tool_id) === selectedTool.id && !l.returnDate
      ).length;

      if (availableUnits < 1) {
        showError(
          `La herramienta "${selectedTool.name}" no tiene unidades disponibles.\n` +
          `Stock total: ${selectedTool.stock} | ` +
          `Prestadas: ${activeLoans} | ` +
          `Disponibles: ${availableUnits}`
        );
        return;
      }
    }

    const selectedClient = clientsMap.get(Number(form.clientId));
    if (selectedClient && selectedClient.state !== "Activo") {
      const stateMessages = {
        "Restringido": `El cliente "${selectedClient.name}" está restringido. Debe regularizar sus préstamos antes de solicitar uno nuevo`,
        "Inactivo": `El cliente "${selectedClient.name}" está inactivo y no puede solicitar préstamos`
      };
      
      showError(stateMessages[selectedClient.state] || `El cliente "${selectedClient.name}" no puede solicitar préstamos (Estado: ${selectedClient.state})`);
      return;
    }

    try {
      setLoading(true);
      await http.post("/api/v1/loans/create", null, {
        params: {
          clientId: form.clientId,
          toolId: form.toolId,
          dueDate: form.dueDate,
        },
      });
      
      showSuccess("Préstamo creado correctamente");
      setForm({ clientId: "", toolId: "", dueDate: "" });
      await fetchAll();
    } catch (err) {
      console.error("Error al crear préstamo:", err);
      const errorMessage = extractErrorMessage(err);
      showError(errorMessage);
    } finally {
      setLoading(false);
    }
  }

  function handleOpenReturnModal(loan) {
    setSelectedLoan(loan);
    setReturnModalOpen(true);
  }

  function handleCloseReturnModal() {
    setReturnModalOpen(false);
    setSelectedLoan(null);
  }

  async function handleReturnLoan(loanId, isDamaged, isIrreparable) {
    try {
      await http.post("/api/v1/loans/return", null, {
        params: { loanId, isDamaged, isIrreparable },
      });
      
      showSuccess("Herramienta devuelta exitosamente");
      handleCloseReturnModal();
      await fetchAll();
    } catch (err) {
      console.error("Error al devolver herramienta:", err);
      const errorMessage = extractErrorMessage(err);
      showError(errorMessage);
      throw err;
    }
  }

  const getStatusChip = (status) => {
    const statusConfig = {
      'Vigente': { color: 'success', icon: <CheckIcon sx={{ fontSize: 16 }} /> },
      'Atrasado': { color: 'error', icon: <ErrorIcon sx={{ fontSize: 16 }} /> },
      'Devuelto': { color: 'info', icon: <CheckIcon sx={{ fontSize: 16 }} /> },
    };

    const config = statusConfig[status] || { color: 'default', icon: null };

    return (
      <Chip
        label={status}
        color={config.color}
        size="small"
        icon={config.icon}
        sx={{ fontWeight: 600 }}
      />
    );
  };

  return (
    <Box sx={{ p: 3, backgroundColor: '#f5f7fa', minHeight: '100vh' }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, color: '#1e293b' }}>
          Gestión de Préstamos
        </Typography>
        
        <Button 
          variant="contained"
          startIcon={<RefreshIcon />}
          onClick={fetchAll} 
          disabled={loading}
          sx={{ 
            backgroundColor: '#2563eb',
            '&:hover': { backgroundColor: '#1d4ed8' },
            textTransform: 'none',
            fontWeight: 600,
            px: 3,
          }}
        >
          {loading ? "Actualizando..." : "Actualizar"}
        </Button>
      </Box>

      {/* Estadísticas */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ backgroundColor: '#10b981', color: 'white' }}>
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Préstamos Activos
              </Typography>
              <Typography variant="h3" sx={{ fontWeight: 700 }}>
                {stats.active}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ backgroundColor: '#ef4444', color: 'white' }}>
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Atrasados
              </Typography>
              <Typography variant="h3" sx={{ fontWeight: 700 }}>
                {stats.overdue}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ backgroundColor: '#3b82f6', color: 'white' }}>
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Devueltos
              </Typography>
              <Typography variant="h3" sx={{ fontWeight: 700 }}>
                {stats.returned}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ backgroundColor: '#f59e0b', color: 'white' }}>
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Multas Totales
              </Typography>
              <Typography variant="h3" sx={{ fontWeight: 700 }}>
                {formatCurrency(stats.totalFines)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Formulario Crear Préstamo */}
      <Paper sx={{ p: 3, mb: 3, borderRadius: 2, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 2, color: '#1e293b' }}>
          Crear Nuevo Préstamo
        </Typography>

        <form onSubmit={handleCreate}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                select
                fullWidth
                label="Cliente"
                value={form.clientId}
                onChange={(e) => setForm({ ...form, clientId: e.target.value })}
                disabled={loading}
                size="small"
              >
                <MenuItem value="">
                  <em>-- Seleccionar Cliente --</em>
                </MenuItem>
                {clients.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    <ListItemText
                      primary={c.name}
                      secondary={`ID: ${c.id} | Estado: ${c.state}`}
                    />
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                select
                fullWidth
                label="Herramienta"
                value={form.toolId}
                onChange={(e) => setForm({ ...form, toolId: e.target.value })}
                disabled={loading}
                size="small"
                InputProps={{
                  startAdornment: <InventoryIcon sx={{ mr: 1, color: '#64748b' }} />,
                }}
              >
                <MenuItem value="">
                  <em>-- Seleccionar Herramienta --</em>
                </MenuItem>
                {tools.map((t) => {
                  const available = getAvailableUnits(t.id);
                  const activeLoans = loans.filter(
                    l => (l.toolId || l.tool?.id || l.tool_id) === t.id && !l.returnDate
                  ).length;
                  const availColor = getAvailabilityColor(available, t.stock);
                  
                  return (
                    <MenuItem key={t.id} value={t.id}>
                      <Box sx={{ display: 'flex', alignItems: 'center', width: '100%', gap: 1 }}>
                        <Box 
                          sx={{ 
                            width: 8, 
                            height: 8, 
                            borderRadius: '50%', 
                            backgroundColor: availColor,
                            flexShrink: 0,
                          }} 
                        />
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                {t.name}
                              </Typography>
                              <Chip 
                                label={`${available}/${t.stock}`} 
                                size="small"
                                sx={{ 
                                  backgroundColor: availColor,
                                  color: 'white',
                                  fontWeight: 600,
                                  height: 20,
                                  fontSize: '0.7rem',
                                }}
                              />
                            </Box>
                          }
                          secondary={
                            <Typography variant="caption" sx={{ color: '#64748b' }}>
                              Stock: {t.stock} | Prestadas: {activeLoans} | Disponibles: {available} | {t.status}
                            </Typography>
                          }
                        />
                      </Box>
                    </MenuItem>
                  );
                })}
              </TextField>
            </Grid>

            {/* ✅ Fecha con TextField type="date" (sin date-pickers) */}
            <Grid item xs={12} md={3}>
              <TextField
                type="date"
                fullWidth
                label="Fecha Límite"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                disabled={loading}
                size="small"
                InputLabelProps={{ shrink: true }}
                inputProps={{ min: new Date().toISOString().split('T')[0] }}
                InputProps={{
                  startAdornment: <CalendarIcon sx={{ mr: 1, color: '#64748b' }} />,
                }}
              />
            </Grid>

            <Grid item xs={12} md={1}>
              <Button
                type="submit"
                variant="contained"
                fullWidth
                disabled={loading}
                sx={{
                  height: '40px',
                  backgroundColor: '#2563eb',
                  '&:hover': { backgroundColor: '#1d4ed8' },
                  textTransform: 'none',
                  fontWeight: 600,
                }}
              >
                {loading ? "..." : "Crear"}
              </Button>
            </Grid>
          </Grid>
        </form>
      </Paper>

      {/* Tabla de Préstamos */}
      <TableContainer component={Paper} sx={{ borderRadius: 2, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <Table size="small">
          <TableHead sx={{ backgroundColor: '#f1f5f9' }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>Cliente</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>Herramienta</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>F. Préstamo</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>F. Límite</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>F. Devolución</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>Costo</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>Multa</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }}>Estado</TableCell>
              <TableCell sx={{ fontWeight: 600, color: '#475569' }} align="center">Acción</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4, color: '#64748b' }}>
                  Cargando...
                </TableCell>
              </TableRow>
            ) : loans.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4, color: '#94a3b8' }}>
                  No hay préstamos registrados
                </TableCell>
              </TableRow>
            ) : (
              loans.map((loan) => (
                <TableRow 
                  key={loan.id}
                  hover
                  sx={{ '&:hover': { backgroundColor: '#f8fafc' } }}
                >
                  <TableCell sx={{ color: '#475569', fontWeight: 500 }}>{loan.id}</TableCell>
                  <TableCell sx={{ color: '#1e293b' }}>{clientLabel(loan)}</TableCell>
                  <TableCell sx={{ color: '#1e293b' }}>{toolLabel(loan)}</TableCell>
                  <TableCell sx={{ color: '#64748b' }}>{getLoanDate(loan)}</TableCell>
                  <TableCell sx={{ color: '#64748b' }}>{loan.dueDate || "—"}</TableCell>
                  <TableCell sx={{ color: '#64748b' }}>{loan.returnDate || loan.return_date || "—"}</TableCell>
                  <TableCell sx={{ color: '#059669', fontWeight: 600 }}>
                    {formatCurrency(loan.rentalCost || loan.rental_cost || 0)}
                  </TableCell>
                  <TableCell sx={{ color: loan.fine > 0 ? '#dc2626' : '#64748b', fontWeight: 600 }}>
                    {formatCurrency(loan.fine || 0)}
                  </TableCell>
                  <TableCell>{getStatusChip(loan.status)}</TableCell>
                  <TableCell align="center">
                    {loan.returnDate || loan.return_date ? (
                      <Chip label="Devuelto" size="small" color="default" />
                    ) : (
                      <Tooltip title="Devolver herramienta">
                        <IconButton
                          onClick={() => handleOpenReturnModal(loan)}
                          size="small"
                          sx={{ 
                            color: '#16a34a',
                            '&:hover': { backgroundColor: '#dcfce7' }
                          }}
                        >
                          <ReturnIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <ReturnLoanModal
        open={returnModalOpen}
        onClose={handleCloseReturnModal}
        loan={selectedLoan}
        onConfirm={handleReturnLoan}
        clientName={selectedLoan ? clientNameOnly(selectedLoan) : ""}
        toolName={selectedLoan ? toolNameOnly(selectedLoan) : ""}
      />
    </Box>
  );
}