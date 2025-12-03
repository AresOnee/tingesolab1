import axios from "axios";
import keycloak from "./services/keycloak";

// 🔥 CORRECCIÓN CRÍTICA: Usar ruta relativa.
// Al usar "/api/v1", el navegador hará la petición a http://localhost/api/v1
// (el mismo dominio donde está la web), evitando el bloqueo CORS.
// Nginx luego tomará esa petición y la enviará al backend internamente.
export const API_BASE = "";

const http = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
});

// Interceptor de Request: Agrega el Token de Keycloak
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

// Función para configurar el callback desde el contexto (App.jsx)
export const setupErrorHandler = (callback) => {
  showErrorCallback = callback;
  console.log("✅ setupErrorHandler configurado");
};

// Interceptor de Response: Manejo de Errores
http.interceptors.response.use(
  (response) => response,
  (error) => {
    console.log("🔍 Error interceptado:", error);
    
    // Extraer información del error
    const status = error.response?.status;
    const backendData = error.response?.data;
    
    // Intentar extraer el mensaje del backend de TODAS las formas posibles
    let message = 
      backendData?.message ||           
      backendData?.error ||              
      backendData?.detail ||             
      (typeof backendData === 'string' ? backendData : null) || 
      error.message ||                   
      'Error desconocido';
    
    // Ajuste para errores de Spring Boot
    if (backendData && typeof backendData === 'object') {
      if (backendData.message && backendData.message !== 'Bad Request') {
        message = backendData.message;
      } else if (backendData.error && backendData.error !== 'Bad Request') {
        message = backendData.error;
      }
    }
    
    let userMessage = message;
    
    // Mejorar mensajes técnicos genéricos
    const isTechnicalError = 
      message.includes("Request failed") || 
      message.includes("Network Error") ||
      message === "Error desconocido" ||
      message === "Bad Request";
    
    if (isTechnicalError) {
      switch (status) {
        case 400: userMessage = "Datos inválidos. Verifica la información."; break;
        case 401: userMessage = "Sesión expirada. Inicia sesión nuevamente."; break;
        case 403: userMessage = "No tienes permisos para realizar esta acción."; break;
        case 404: userMessage = "Recurso no encontrado."; break;
        case 409: userMessage = "El recurso ya existe o está en conflicto."; break;
        case 500: userMessage = "Error interno del servidor."; break;
        case 503: userMessage = "Servicio no disponible."; break;
        default:
          if (message.includes("Network Error")) {
            userMessage = "Error de conexión. Verifica que el servidor esté funcionando.";
          }
      }
    }

    // Agregar emojis si es un mensaje corto
    const isDetailedMessage = userMessage.includes('\n') || userMessage.length > 100;
    if (!isDetailedMessage) {
      const emojiMap = { 400: "⚠️", 401: "🔒", 403: "⛔", 404: "❌", 409: "⚠️", 500: "❌" };
      const emoji = emojiMap[status] || "⚠️";
      userMessage = `${emoji} ${userMessage}`;
    }
    
    // Objeto de error enriquecido
    const enrichedError = {
      ...error,
      userMessage,
      originalMessage: message,
      status,
      shownToUser: Boolean(showErrorCallback),
    };
    
    // Mostrar error en UI si existe el callback
    if (showErrorCallback) {
      showErrorCallback(userMessage);
    }
    
    return Promise.reject(enrichedError);
  }
);

export default http;