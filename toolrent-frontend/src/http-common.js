import axios from "axios";
import keycloak from "./services/keycloak";

// Mantén las mismas variables que ya usabas (o default a localhost:8090)
const SERVER = import.meta.env.VITE_PAYROLL_BACKEND_SERVER || "localhost";
const PORT   = import.meta.env.VITE_PAYROLL_BACKEND_PORT   || "8090";

/**
 * 🔴 OJO: baseURL SIN /api/v1
 * Dejamos la base en la raíz del backend para que los servicios que llaman
 * a "/api/v1/..." no queden como "/api/v1/api/v1/...".
 */
export const API_BASE = `http://${SERVER}:${PORT}`;

const http = axios.create({
  baseURL: API_BASE,               // <-- sin /api/v1
  headers: { "Content-Type": "application/json" },
});

http.interceptors.request.use(async (config) => {
  if (keycloak?.authenticated) {
    try { await keycloak.updateToken(30); } catch {}
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  } else {
    const t = localStorage.getItem("kc_token");
    if (t) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${t}`;
    }
  }
  return config;
});

export default http;
