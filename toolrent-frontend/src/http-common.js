import axios from "axios";
import keycloak from "./services/keycloak";

// o default a localhost:8090
const SERVER = import.meta.env.VITE_PAYROLL_BACKEND_SERVER || "localhost";
const PORT   = import.meta.env.VITE_PAYROLL_BACKEND_PORT   || "8090";

export const API_BASE = `http://${SERVER}:${PORT}`;

const http = axios.create({
  baseURL: API_BASE,
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

// Variable para almacenar la función showError del contexto
let showErrorCallback = null;

// Función para configurar el callback desde el contexto
export const setupErrorHandler = (callback) => {
  showErrorCallback = callback;
  console.log("✅ setupErrorHandler configurado");
};

// Interceptor de respuestas para errores HTTP - MEJORADO
http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.log("🔍 Error interceptado:", error);
    console.log("📦 Response completa:", error.response);
    console.log("📊 Response data:", error.response?.data);
    
    // Extraer información del error
    const status = error.response?.status;
    const backendData = error.response?.data;
    
    // Intentar extraer el mensaje del backend de TODAS las formas posibles
    let message = 
      backendData?.message ||           // Spring Boot: { message: "..." }
      backendData?.error ||              // Algunos frameworks: { error: "..." }
      backendData?.detail ||             // REST estándar: { detail: "..." }
      backendData?.errors?.[0] ||        // Array de errores: { errors: ["..."] }
      backendData?.errors?.[0]?.message || // Array de objetos: { errors: [{message: "..."}] }
      (typeof backendData === 'string' ? backendData : null) || // String directo
      error.message ||                   // Mensaje de Axios
      'Error desconocido';
    
    console.log("💬 Mensaje extraído del backend:", message);
    
    // Si el mensaje sigue siendo técnico de Axios, mejorarlo
    let userMessage = message;
    
    if (message.includes("Request failed with status code") || 
        message.includes("Network Error") ||
        message === "Error desconocido") {
      
      // Mensajes genéricos mejorados por código de error
      switch (status) {
        case 400:
          userMessage = "Datos inválidos. Por favor verifica la información ingresada.";
          break;
        case 401:
          userMessage = "Sesión expirada. Por favor inicia sesión nuevamente.";
          break;
        case 403:
          userMessage = "No tienes permisos para realizar esta acción.";
          break;
        case 404:
          userMessage = "Recurso no encontrado.";
          break;
        case 409:
          userMessage = "El recurso ya existe o está en conflicto.";
          break;
        case 422:
          userMessage = "Los datos enviados no son válidos.";
          break;
        case 500:
          userMessage = "Error interno del servidor. Por favor intenta más tarde.";
          break;
        case 503:
          userMessage = "Servicio temporalmente no disponible.";
          break;
        default:
          if (message.includes("Network Error")) {
            userMessage = "Error de conexión. Verifica que el servidor esté funcionando.";
          } else {
            userMessage = `Error ${status || 'desconocido'}`;
          }
      }
    }
    
    // Agregar emoji según el tipo de error
    const emojiMap = {
      400: "⚠️",
      401: "🔒",
      403: "⛔",
      404: "❌",
      409: "⚠️",
      422: "⚠️",
      500: "❌",
      503: "⏳"
    };
    
    const emoji = emojiMap[status] || "⚠️";
    userMessage = `${emoji} ${userMessage}`;
    
    console.log("👤 Mensaje final para usuario:", userMessage);
    
    // Mostrar error en Snackbar si el callback está configurado
    if (showErrorCallback) {
      showErrorCallback(userMessage);
    } else {
      console.warn("⚠️ showErrorCallback NO configurado - error no se mostrará en UI");
      console.warn("💡 Asegúrate de llamar setupErrorHandler(showError) en App.jsx");
    }
    
    // Rechazar la promesa para que el catch funcione
    return Promise.reject(error);
  }
);

export default http;