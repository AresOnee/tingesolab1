// src/components/Loans.jsx
// VERSION FINAL - Sin console.log, con soporte para objetos anidados

import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import React, { useEffect, useMemo, useState } from "react";
import http from "../http-common";
import ReturnLoanModal from "./ReturnLoanModal";
import { useSnackbar } from "../contexts/SnackbarContext";

export default function Loans() {
  // Contexto de notificaciones
  const { showSuccess, showError, showWarning } = useSnackbar();

  // catalogos
  const [clients, setClients] = useState([]);
  const [tools, setTools] = useState([]);
  // tabla
  const [loans, setLoans] = useState([]);
  // formulario
  const [form, setForm] = useState({ clientId: "", toolId: "", dueDate: "" });
  const [loading, setLoading] = useState(false);

  // Estado para el modal de devolucion
  const [returnModalOpen, setReturnModalOpen] = useState(false);
  const [selectedLoan, setSelectedLoan] = useState(null);

  // mapas para resolver rapidamente nombres por id
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

  // FUNCIONES CON SOPORTE PARA OBJETOS ANIDADOS Y SNAKE_CASE
  const clientLabel = (loan) => {
    // Si client es un objeto completo con datos
    if (loan.client && typeof loan.client === 'object' && loan.client.name) {
      return `${loan.client.id} - ${loan.client.name}`;
    }
    
    // Si es solo un ID (camelCase o snake_case)
    const id = loan.clientId || loan.client_id || loan.client?.id;
    
    if (!id) return "—";
    
    const client = clientsMap.get(Number(id));
    return client ? `${id} - ${client.name}` : `${id} - N/A`;
  };

  const toolLabel = (loan) => {
    // Si tool es un objeto completo con datos
    if (loan.tool && typeof loan.tool === 'object' && loan.tool.name) {
      return `${loan.tool.id} - ${loan.tool.name}`;
    }
    
    // Si es solo un ID (camelCase o snake_case)
    const id = loan.toolId || loan.tool_id || loan.tool?.id;
    
    if (!id) return "—";
    
    const tool = toolsMap.get(Number(id));
    return tool ? `${id} - ${tool.name}` : `${id} - N/A`;
  };

  const getLoanDate = (loan) => {
    return loan.loanDate || loan.loan_date || loan.startDate || "—";
  };

  // Formatear moneda chilena
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

      // Para cada respuesta, verifica si es una lista directa o un objeto con propiedad "content"
      const clientsData = Array.isArray(cRes.data) ? cRes.data : (cRes.data?.content ?? []);
      const toolsData = Array.isArray(tRes.data) ? tRes.data : (tRes.data?.content ?? []);
      const loansData = Array.isArray(lRes.data) ? lRes.data : (lRes.data?.content ?? []);

      setClients(clientsData);
      setTools(toolsData);
      setLoans(loansData);

    } catch (err) {
      console.error("Error cargando datos:", err);
      showError("Error cargando datos. Ver consola para detalles.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(e) {
    e.preventDefault();
    
    if (!form.clientId || !form.toolId || !form.dueDate) {
      showWarning("Por favor completa todos los campos: Cliente, Herramienta y Fecha de devolucion");
      return;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const dueDate = new Date(form.dueDate);
    if (isNaN(dueDate.getTime())) {
      showWarning("Formato de fecha invalido. Por favor selecciona una fecha valida");
      return;
    }

    if (dueDate < today) {
      showWarning("La fecha de devolucion no puede ser anterior a hoy");
      return;
    }

    const selectedTool = tools.find(t => t.id === Number(form.toolId));
    if (!selectedTool) {
      showError("La herramienta seleccionada no existe");
      return;
    }

    if (selectedTool.stock <= 0) {
      showWarning(`La herramienta "${selectedTool.name}" no tiene stock disponible (Stock actual: ${selectedTool.stock})`);
      return;
    }

    if (selectedTool.status !== "Disponible") {
      showWarning(`La herramienta "${selectedTool.name}" no esta disponible (Estado: ${selectedTool.status})`);
      return;
    }

    try {
      const body = new URLSearchParams();
      body.append("clientId", form.clientId);
      body.append("toolId", form.toolId);
      body.append("dueDate", form.dueDate);

      await http.post("/api/v1/loans/create", body, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      });

      setForm({ clientId: "", toolId: "", dueDate: "" });
      await fetchAll();
      showSuccess("Prestamo creado exitosamente");
    } catch (err) {
      console.error("❌ Error al crear prestamo:", err);
      console.error("📦 Response data:", err.response?.data);
      console.error("📊 Status:", err.response?.status);
      
      // Si el interceptor no mostró el error, mostrarlo manualmente
      if (err.response?.data?.message) {
        showError(`Error al crear préstamo: ${err.response.data.message}`);
      } else if (!err.response) {
        showError("Error de conexión. Verifica que el backend esté funcionando.");
      }
      // Si el interceptor ya lo mostró, no hacer nada más
    }
  }

  function handleOpenReturnModal(loan) {
    // Extraer IDs de forma flexible (objetos anidados o IDs directos)
    const clientId = loan.client?.id || loan.clientId || loan.client_id;
    const toolId = loan.tool?.id || loan.toolId || loan.tool_id;
    
    const enrichedLoan = {
      ...loan,
      clientId: clientId,
      toolId: toolId,
      clientName: loan.client?.name || clientsMap.get(clientId)?.name || 'N/A',
      toolName: loan.tool?.name || toolsMap.get(toolId)?.name || 'N/A',
      replacementValue: loan.tool?.replacementValue || toolsMap.get(toolId)?.replacementValue || 0,
    };
    setSelectedLoan(enrichedLoan);
    setReturnModalOpen(true);
  }

  function handleCloseReturnModal() {
    setReturnModalOpen(false);
    setSelectedLoan(null);
  }

  async function handleReturnSuccess() {
    await fetchAll();
    showSuccess("Prestamo devuelto exitosamente");
  }

  const Status = ({ value }) => {
    const color =
      value === "Vigente"
        ? "#16a34a"
        : value === "Atrasado"
        ? "#d97706"
        : value === "Devuelto"
        ? "#64748b"
        : "#9ca3af";

    return (
      <span
        style={{
          background: color,
          color: "#fff",
          padding: "2px 8px",
          borderRadius: 4,
          fontSize: 12,
          fontWeight: 600,
        }}
      >
        {value}
      </span>
    );
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Gestion de Prestamos
      </Typography>

      {/* FORMULARIO CREAR PRESTAMO */}
      <div
        style={{
          background: "#fff",
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
          marginBottom: 24,
        }}
      >
        <h3 style={{ fontSize: 18, fontWeight: 600, marginBottom: 12 }}>
          Crear Prestamo
        </h3>

        <form
          onSubmit={handleCreate}
          style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr auto", gap: 12 }}
        >
          <div>
            <label style={labelStyle}>Cliente</label>
            <select
              style={inputStyle}
              value={form.clientId}
              onChange={(e) => setForm({ ...form, clientId: e.target.value })}
            >
              <option value="">-- Seleccionar --</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.id} - {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label style={labelStyle}>Herramienta</label>
            <select
              style={inputStyle}
              value={form.toolId}
              onChange={(e) => setForm({ ...form, toolId: e.target.value })}
            >
              <option value="">-- Seleccionar --</option>
              {tools
                .filter(t => t.status === "Disponible" && t.stock > 0)
                .map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.id} - {t.name} (Stock: {t.stock})
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label style={labelStyle}>Fecha Devolucion</label>
            <input
              type="date"
              style={inputStyle}
              value={form.dueDate}
              onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
            />
          </div>

          <div style={{ alignSelf: "end" }}>
            <button type="submit" style={primaryBtn} disabled={loading}>
              Crear
            </button>
          </div>
        </form>
      </div>

      {/* TABLA PRESTAMOS ACTIVOS */}
      <div
        style={{
          background: "#fff",
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
        }}
      >
        <h3 style={{ fontSize: 18, fontWeight: 600, marginBottom: 12 }}>
          Prestamos Activos
        </h3>

        <table style={tableStyle}>
          <thead>
            <tr>
              <Th>ID</Th>
              <Th>Cliente</Th>
              <Th>Herramienta</Th>
              <Th>Fecha Prestamo</Th>
              <Th>Fecha Limite</Th>
              <Th>Fecha Devolucion</Th>
              <Th>Costo Arriendo</Th>
              <Th>Multa</Th>
              <Th>Danado</Th>
              <Th>Irreparable</Th>
              <Th>Estado</Th>
              <Th>Accion</Th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <Td colSpan={12} style={{ textAlign: "center" }}>
                  Cargando...
                </Td>
              </tr>
            ) : loans.length === 0 ? (
              <tr>
                <Td colSpan={12} style={{ textAlign: "center", color: "#94a3b8" }}>
                  No hay prestamos registrados
                </Td>
              </tr>
            ) : (
              loans.map((loan) => {
                return (
                  <tr key={loan.id} style={{ borderBottom: "1px solid #e2e8f0" }}>
                    <Td>{loan.id}</Td>
                    <Td>{clientLabel(loan)}</Td>
                    <Td>{toolLabel(loan)}</Td>
                    <Td>{getLoanDate(loan)}</Td>
                    <Td>{loan.dueDate || loan.due_date || "—"}</Td>
                    <Td>{loan.returnDate || loan.return_date || "—"}</Td>
                    <Td>{formatCurrency(loan.rentalCost || loan.rental_cost)}</Td>
                    <Td style={{ color: (loan.fine || 0) > 0 ? "#dc2626" : "#64748b" }}>
                      {formatCurrency(loan.fine)}
                    </Td>
                    <Td>
                      {loan.damaged === true
                        ? "Si"
                        : loan.damaged === false
                        ? "No"
                        : ""}
                    </Td>
                    <Td>
                      {loan.irreparable === true
                        ? "Si"
                        : loan.irreparable === false
                        ? "No"
                        : ""}
                    </Td>
                    <Td>
                      <Status value={loan.status} />
                    </Td>
                    <Td>
                      {loan.returnDate || loan.return_date ? (
                        <span style={{ color: "#64748b" }}>Devuelto</span>
                      ) : (
                        <button
                          onClick={() => handleOpenReturnModal(loan)}
                          style={returnBtn}
                        >
                          Devolver
                        </button>
                      )}
                    </Td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <ReturnLoanModal
        open={returnModalOpen}
        onClose={handleCloseReturnModal}
        loan={selectedLoan}
        onSuccess={handleReturnSuccess}
      />
    </Box>
  );
}

// ========== ESTILOS ==========
const labelStyle = {
  display: "block",
  fontSize: 13,
  fontWeight: 500,
  marginBottom: 4,
};

const inputStyle = {
  width: "100%",
  padding: "8px 12px",
  border: "1px solid #cbd5e1",
  borderRadius: 6,
  fontSize: 14,
};

const primaryBtn = {
  background: "#2563eb",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  padding: "8px 16px",
  cursor: "pointer",
  fontWeight: 600,
  fontSize: 14,
};

const returnBtn = {
  background: "#16a34a",
  color: "#fff",
  border: "none",
  borderRadius: 4,
  padding: "4px 12px",
  cursor: "pointer",
  fontSize: 13,
};

const tableStyle = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: 14,
};

const Th = ({ children }) => (
  <th
    style={{
      background: "#f1f5f9",
      padding: "8px 12px",
      textAlign: "left",
      fontWeight: 600,
      borderBottom: "2px solid #cbd5e1",
    }}
  >
    {children}
  </th>
);

const Td = ({ children, colSpan, style }) => (
  <td
    colSpan={colSpan}
    style={{
      padding: "8px 12px",
      borderBottom: "1px solid #e2e8f0",
      ...style,
    }}
  >
    {children}
  </td>
);