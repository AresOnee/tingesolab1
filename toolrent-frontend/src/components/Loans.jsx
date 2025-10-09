// src/components/Loans.jsx
import React, { useEffect, useMemo, useState } from "react";
import api from "../http-common";

/**
 * Préstamos
 * - Muestra "ID - Nombre" para Cliente y Herramienta aunque el backend solo entregue clientId/toolId.
 * - Crea préstamos con form-urlencoded.
 * - Devuelve préstamos con banderas de daño / irreparable.
 * - MEJORA: Muestra costo del arriendo
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

  async function handleCreate(e) {
    e.preventDefault();
    if (!form.clientId || !form.toolId || !form.dueDate) {
      alert("Completa Cliente, Herramienta y Fecha devolución.");
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
    } catch (err) {
      console.error("Error al crear préstamo:", err);
      alert(err?.response?.data?.message || err.message);
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
    } catch (err) {
      console.error("Error al devolver préstamo:", err);
      alert(err?.response?.data?.message || err.message);
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
          <label style={labelStyle}>Cliente</label>
          <select
            value={form.clientId}
            onChange={(e) => setForm((f) => ({ ...f, clientId: e.target.value }))}
            style={inputStyle}
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
          <label style={labelStyle}>Herramienta (Stock &gt;= 0)</label>
          <select
            value={form.toolId}
            onChange={(e) => setForm((f) => ({ ...f, toolId: e.target.value }))}
            style={inputStyle}
          >
            <option value="">Seleccione</option>
            {tools
              .filter((t) => (typeof t.stock === "number" ? t.stock >= 0 : true))
              .map((t) => (
                <option key={t.id} value={t.id}>
                  {t.id} - {t.name}
                </option>
              ))}
          </select>
        </div>

        <div>
          <label style={labelStyle}>Fecha devolución</label>
          <input
            type="date"
            value={form.dueDate}
            onChange={(e) => setForm((f) => ({ ...f, dueDate: e.target.value }))}
            style={inputStyle}
          />
        </div>

        <button type="submit" style={primaryBtn} disabled={loading}>
          CREAR
        </button>
      </form>

      <h2 style={{ margin: "0 0 12px" }}>Préstamos Activos</h2>

      <div style={{ overflowX: "auto" }}>
        <table style={tableStyle}>
          <thead>
            <tr>
              <Th>ID Préstamo</Th>
              <Th>ID Cliente</Th>
              <Th>ID Herramienta</Th>
              <Th>F. Préstamo</Th>
              <Th>F. Límite</Th>
              <Th>F. Devolución</Th>
              <Th>Costo Arriendo</Th>{/* NUEVA COLUMNA */}
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
                    {/* NUEVA CELDA: Costo Arriendo */}
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
                        <button style={mutedBtn} disabled>
                          Devuelto
                        </button>
                      ) : (
                        <button
                          style={secondaryBtn}
                          onClick={() => handleReturn(loan.id)}
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

/* ---------- estilos pequeños sin librerías ---------- */
const labelStyle = { display: "block", fontSize: 12, marginBottom: 6, color: "#374151" };
const inputStyle = {
  width: "100%",
  padding: "9px 10px",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  outline: "none",
};
const tableStyle = { width: "100%", borderCollapse: "collapse" };
const Th = ({ children }) => (
  <th
    style={{
      textAlign: "left",
      padding: "10px 8px",
      borderBottom: "1px solid #e5e7eb",
      fontWeight: 600,
      whiteSpace: "nowrap",
      color: "#111827",
      background: "#f9fafb",
    }}
  >
    {children}
  </th>
);
const Td = ({ children, ...props }) => (
  <td
    {...props}
    style={{
      padding: "8px",
      borderBottom: "1px solid #f3f4f6",
      whiteSpace: "nowrap",
      color: "#111827",
      ...props.style,
    }}
  >
    {children}
  </td>
);
const primaryBtn = {
  padding: "10px 16px",
  borderRadius: 8,
  border: "1px solid #2563eb",
  background: "#2563eb",
  color: "white",
  fontWeight: 600,
  cursor: "pointer",
};
const secondaryBtn = {
  padding: "6px 10px",
  borderRadius: 6,
  border: "1px solid #2563eb",
  background: "transparent",
  color: "#2563eb",
  cursor: "pointer",
  fontWeight: 600,
};
const mutedBtn = {
  padding: "6px 10px",
  borderRadius: 6,
  border: "1px solid #9ca3af",
  background: "transparent",
  color: "#6b7280",
  fontWeight: 600,
};