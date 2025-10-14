// src/components/Loans.jsx
// ✅ VERSIÓN MEJORADA CON VALIDACIONES (Punto 3 del Gap Analysis)

import React, { useEffect, useMemo, useState } from "react";
import api from "../http-common";

/**
 * Préstamos - Con validaciones mejoradas
 * MEJORAS IMPLEMENTADAS:
 * ✅ Validación: dueDate debe ser >= hoy
 * ✅ Validación: Herramienta debe tener stock > 0
 * ✅ Mensajes de error claros y descriptivos
 * ✅ Validación de formato de fecha
 */
export default function Loans() {
  // catálogos
  const [clients, setClients] = useState([]);
  const [tools, setTools] = useState([]);
  // tabla
  const [loans, setLoans] = useState([]);
  // formulario
  const [form, setForm] = useState({ clientId: "", toolId: "", dueDate: "" });
  const [loading, setLoading] = useState(false);

  // mapas para resolver rápidamente nombres por id
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

  const clientLabel = (id) =>
    id ? `${id} - ${clientsMap.get(Number(id))?.name ?? "N/A"}` : "—";

  const toolLabel = (id) =>
    id ? `${id} - ${toolsMap.get(Number(id))?.name ?? "N/A"}` : "—";

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
        api.get("/api/v1/clients/"),
        api.get("/api/v1/tools/"),
        api.get("/api/v1/loans/"),
      ]);

      setClients(Array.isArray(cRes.data) ? cRes.data : []);
      setTools(Array.isArray(tRes.data) ? tRes.data : []);
      setLoans(Array.isArray(lRes.data) ? lRes.data : []);
    } catch (err) {
      console.error("Error cargando datos:", err);
      alert("Error cargando datos (ver consola).");
    } finally {
      setLoading(false);
    }
  }

  // ✅ FUNCIÓN MEJORADA CON VALIDACIONES
  async function handleCreate(e) {
    e.preventDefault();
    
    // ✅ Validación 1: Campos obligatorios
    if (!form.clientId || !form.toolId || !form.dueDate) {
      alert("⚠️ Por favor completa todos los campos: Cliente, Herramienta y Fecha de devolución.");
      return;
    }

    // ✅ Validación 2: Fecha de devolución debe ser posterior o igual a hoy
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Resetear horas para comparar solo fechas
    
    const dueDate = new Date(form.dueDate);
    if (isNaN(dueDate.getTime())) {
      alert("⚠️ Formato de fecha inválido. Por favor selecciona una fecha válida.");
      return;
    }

    if (dueDate < today) {
      alert("⚠️ La fecha de devolución no puede ser anterior a hoy.");
      return;
    }

    // ✅ Validación 3: Verificar que la herramienta tenga stock > 0
    const selectedTool = tools.find(t => t.id === Number(form.toolId));
    if (!selectedTool) {
      alert("⚠️ La herramienta seleccionada no existe.");
      return;
    }

    if (selectedTool.stock <= 0) {
      alert(`⚠️ La herramienta "${selectedTool.name}" no tiene stock disponible (Stock actual: ${selectedTool.stock}).`);
      return;
    }

    // ✅ Validación 4: Verificar que la herramienta esté disponible
    if (selectedTool.status !== "Disponible") {
      alert(`⚠️ La herramienta "${selectedTool.name}" no está disponible (Estado: ${selectedTool.status}).`);
      return;
    }

    try {
      const body = new URLSearchParams();
      body.append("clientId", form.clientId);
      body.append("toolId", form.toolId);
      body.append("dueDate", form.dueDate);

      await api.post("/api/v1/loans/create", body, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      });

      setForm({ clientId: "", toolId: "", dueDate: "" });
      await fetchAll();
      alert("✅ Préstamo creado exitosamente");
    } catch (err) {
      console.error("Error al crear préstamo:", err);
      const errorMsg = err?.response?.data?.message || err.message || "Error desconocido";
      alert(`❌ Error al crear préstamo: ${errorMsg}`);
    }
  }

  async function handleReturn(loanId) {
    const isDamaged = window.confirm(
      "¿El ítem está dañado? (Aceptar = Sí / Cancelar = No)"
    );
    let isIrreparable = false;
    if (isDamaged) {
      isIrreparable = window.confirm("¿El daño es irreparable?");
    }

    try {
      const body = new URLSearchParams();
      body.append("loanId", loanId);
      body.append("isDamaged", String(isDamaged));
      body.append("isIrreparable", String(isIrreparable));

      await api.post("/api/v1/loans/return", body, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      });

      await fetchAll();
      alert("✅ Préstamo devuelto exitosamente");
    } catch (err) {
      console.error("Error al devolver préstamo:", err);
      const errorMsg = err?.response?.data?.message || err.message;
      alert(`❌ Error al devolver: ${errorMsg}`);
    }
  }

  // utilitaria para badge de estado
  const Status = ({ value }) => {
    const color =
      value === "Vigente"
        ? "#16a34a"
        : value === "Atrasado"
        ? "#d97706"
        : value === "Devuelto"
        ? "#2563eb"
        : "#64748b";
    return (
      <span
        style={{
          display: "inline-block",
          padding: "3px 8px",
          borderRadius: 999,
          background: `${color}20`,
          color,
          fontSize: 12,
          fontWeight: 600,
        }}
      >
        {value}
      </span>
    );
  };

  return (
    <div style={{ padding: 16 }}>
      <h2 style={{ margin: "8px 0 16px" }}>Crear Préstamo</h2>

      <form
        onSubmit={handleCreate}
        style={{
          display: "grid",
          gap: 12,
          gridTemplateColumns: "minmax(260px,1fr) minmax(260px,1fr) 180px auto",
          alignItems: "end",
          marginBottom: 24,
        }}
      >
        <div>
          <label style={labelStyle}>Cliente *</label>
          <select
            value={form.clientId}
            onChange={(e) => setForm((f) => ({ ...f, clientId: e.target.value }))}
            style={inputStyle}
            required
          >
            <option value="">Seleccione</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.id} - {c.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label style={labelStyle}>Herramienta (Solo con Stock &gt; 0) *</label>
          <select
            value={form.toolId}
            onChange={(e) => setForm((f) => ({ ...f, toolId: e.target.value }))}
            style={inputStyle}
            required
          >
            <option value="">Seleccione</option>
            {tools
              .filter((t) => t.stock > 0 && t.status === "Disponible") // ✅ Filtro mejorado
              .map((t) => (
                <option key={t.id} value={t.id}>
                  {t.id} - {t.name} (Stock: {t.stock})
                </option>
              ))}
          </select>
        </div>

        <div>
          <label style={labelStyle}>Fecha devolución *</label>
          <input
            type="date"
            value={form.dueDate}
            onChange={(e) => setForm((f) => ({ ...f, dueDate: e.target.value }))}
            style={inputStyle}
            min={new Date().toISOString().split('T')[0]} // ✅ Prevenir fechas pasadas desde el input
            required
          />
        </div>

        <button type="submit" style={primaryBtn} disabled={loading}>
          {loading ? "..." : "CREAR"}
        </button>
      </form>

      <h2 style={{ margin: "0 0 12px" }}>Préstamos Activos</h2>

      <div style={{ overflowX: "auto" }}>
        <table style={tableStyle}>
          <thead>
            <tr>
              <Th>ID</Th>
              <Th>Cliente</Th>
              <Th>Herramienta</Th>
              <Th>F. Préstamo</Th>
              <Th>F. Límite</Th>
              <Th>F. Devolución</Th>
              <Th>Costo Arriendo</Th>
              <Th>Multa</Th>
              <Th>Dañado</Th>
              <Th>Irreparable</Th>
              <Th>Estado</Th>
              <Th>Acciones</Th>
            </tr>
          </thead>
          <tbody>
            {loans.length === 0 ? (
              <tr>
                <Td colSpan={12} style={{ textAlign: "center" }}>
                  {loading ? "Cargando…" : "No hay préstamos."}
                </Td>
              </tr>
            ) : (
              loans.map((loan) => {
                const cid =
                  loan.client?.id ?? loan.clientId ?? loan.client_id ?? null;
                const tid = loan.tool?.id ?? loan.toolId ?? loan.tool_id ?? null;

                return (
                  <tr key={loan.id}>
                    <Td>{loan.id}</Td>
                    <Td>{clientLabel(cid)}</Td>
                    <Td>{toolLabel(tid)}</Td>
                    <Td>{loan.startDate ?? ""}</Td>
                    <Td>{loan.dueDate ?? ""}</Td>
                    <Td>{loan.returnDate ?? ""}</Td>
                    <Td style={{ fontWeight: 600, color: '#2563eb' }}>
                      {formatCurrency(loan.rentalCost)}
                    </Td>
                    <Td>{formatCurrency(loan.fine)}</Td>
                    <Td>
                      {loan.damaged === true
                        ? "Sí"
                        : loan.damaged === false
                        ? "No"
                        : ""}
                    </Td>
                    <Td>
                      {loan.irreparable === true
                        ? "Sí"
                        : loan.irreparable === false
                        ? "No"
                        : ""}
                    </Td>
                    <Td>
                      <Status value={loan.status} />
                    </Td>
                    <Td>
                      {loan.returnDate ? (
                        <span style={{ color: "#64748b" }}>Devuelto</span>
                      ) : (
                        <button
                          onClick={() => handleReturn(loan.id)}
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
    </div>
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