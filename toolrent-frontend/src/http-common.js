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

// ✅ MEJORADO: Interceptor de respuestas con mejor manejo de errores detallados
http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.log("🔍 Error interceptado:", error);
    console.log("📦 Response completa:", error.response);
    console.log("📊 Response data:", error.response?.data);
    
    // Extraer informacion del error
    const status = error.response?.status;
    const backendData = error.response?.data;
    
    // ✅ MEJORADO: Intentar extraer el mensaje del backend de TODAS las formas posibles
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
    
    // ✅ NUEVO: Si el mensaje es de Spring Boot ResponseStatusException
    // Spring devuelve: { "timestamp": "...", "status": 400, "error": "Bad Request", "message": "El mensaje real aquí", "path": "..." }
    if (backendData && typeof backendData === 'object') {
      // Priorizar el campo "message" de Spring Boot
      if (backendData.message && backendData.message !== 'Bad Request') {
        message = backendData.message;
      }
      // Si no, intentar con "error"
      else if (backendData.error && backendData.error !== 'Bad Request') {
        message = backendData.error;
      }
    }
    
    // ✅ MEJORADO: Preservar saltos de línea y formato del backend
    let userMessage = message;
    
    // Solo mejorar mensajes técnicos genéricos, NO los mensajes específicos del backend
    const isTechnicalError = 
      message.includes("Request failed with status code") || 
      message.includes("Network Error") ||
      message === "Error desconocido" ||
      message === "Bad Request" ||
      message === "Internal Server Error";
    
    if (isTechnicalError) {
      // Mensajes genericos mejorados por codigo de error
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
    // ✅ NUEVO: Si el mensaje viene del backend y es específico, usarlo tal cual
    else {
      // Preservar el mensaje exacto del backend (con saltos de línea, detalles, etc.)
      userMessage = message;
    }
    
    // ✅ MEJORADO: Agregar emoji solo si no es un mensaje largo/detallado del backend
    const isDetailedMessage = userMessage.includes('\n') || userMessage.length > 100;
    
    if (!isDetailedMessage) {
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
    }
    
    console.log("👤 Mensaje final para usuario:", userMessage);
    
    // ✅ MEJORADO: Crear objeto de error enriquecido
    const enrichedError = {
      ...error,
      userMessage,           // Mensaje para mostrar al usuario
      originalMessage: message, // Mensaje original del backend
      status,                // Código HTTP
      isDetailedMessage,      // Flag para indicar si es mensaje detallado
      shownToUser: Boolean(showErrorCallback),
    };
    
    // Mostrar error en Snackbar si el callback esta configurado
    if (showErrorCallback) {
      showErrorCallback(userMessage);
    } else {
      console.warn("⚠️ showErrorCallback NO configurado - error no se mostrará en UI");
      console.warn("💡 Asegurate de llamar setupErrorHandler(showError) en App.jsx");
    }
    
    // Rechazar la promesa con el error enriquecido
    return Promise.reject(enrichedError);
  }
);

export default http;