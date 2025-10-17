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

// Variable para almacenar la funcion showError del contexto
let showErrorCallback = null;

// Funcion para configurar el callback desde el contexto
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
    
    // Extraer informacion del error
    const status = error.response?.status;
    const backendData = error.response?.data;
    
    // Intentar extraer el mensaje del backend de TODAS las formas posibles
    let message = 
      backendData?.message ||           // Spring Boot: { message: "..." }
      backendData?.error ||              // Algunos frameworks: { error: "..." }
      backendData?.detail ||             // REST estandar: { detail: "..." }
      backendData?.errors?.[0] ||        // Array de errores: { errors: ["..."] }
      backendData?.errors?.[0]?.message || // Array de objetos: { errors: [{message: "..."}] }
      (typeof backendData === 'string' ? backendData : null) || // String directo
      error.message ||                   // Mensaje de Axios
      'Error desconocido';
    
    console.log("💬 Mensaje extraido del backend:", message);
    
    // Si el mensaje sigue siendo tecnico de Axios, mejorarlo
    let userMessage = message;
    
    if (message.includes("Request failed with status code") || 
        message.includes("Network Error") ||
        message === "Error desconocido") {
      
      // Mensajes genericos mejorados por codigo de error
      switch (status) {
        case 400:
          userMessage = "Datos invalidos. Por favor verifica la informacion ingresada.";
          break;
        case 401:
          userMessage = "Sesion expirada. Por favor inicia sesion nuevamente.";
          break;
        case 403:
          userMessage = "No tienes permisos para realizar esta accion.";
          break;
        case 404:
          userMessage = "Recurso no encontrado.";
          break;
        case 409:
          userMessage = "El recurso ya existe o esta en conflicto.";
          break;
        case 422:
          userMessage = "Los datos enviados no son validos.";
          break;
        case 500:
          userMessage = "Error interno del servidor. Por favor intenta mas tarde.";
          break;
        case 503:
          userMessage = "Servicio temporalmente no disponible.";
          break;
        default:
          if (message.includes("Network Error")) {
            userMessage = "Error de conexion. Verifica que el servidor este funcionando.";
          } else {
            userMessage = `Error ${status || 'desconocido'}`;
          }
      }
    }
    
    // Agregar emoji segun el tipo de error
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
    
    // Mostrar error en Snackbar si el callback esta configurado
    if (showErrorCallback) {
      showErrorCallback(userMessage);
    } else {
      console.warn("⚠️ showErrorCallback NO configurado - error no se mostrara en UI");
      console.warn("💡 Asegurate de llamar setupErrorHandler(showError) en App.jsx");
    }
    
    // Rechazar la promesa para que el catch funcione
    return Promise.reject(error);
  }
);

export default http;