// src/http-common-with-interceptor.js
import axios from 'axios';

const http = axios.create({
  baseURL: 'http://localhost:8090/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Variable para almacenar la función showError del contexto
let showErrorCallback = null;

// Función para configurar el callback desde el contexto
export const setupErrorHandler = (callback) => {
  showErrorCallback = callback;
};

// Interceptor de respuestas para errores HTTP
http.interceptors.response.use(
  (response) => response, // Respuestas exitosas pasan sin cambios
  (error) => {
    // Extraer información del error
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;
    
    // Mapear errores comunes a mensajes amigables
    let userMessage = message;
    
    switch (status) {
      case 400:
        userMessage = `⚠️ Solicitud inválida: ${message}`;
        break;
      case 401:
        userMessage = '🔒 No autorizado. Por favor inicia sesión.';
        break;
      case 403:
        userMessage = '⛔ No tienes permisos para realizar esta acción.';
        break;
      case 404:
        userMessage = `❌ No encontrado: ${message}`;
        break;
      case 409:
        // Error de conflicto (duplicados, etc.)
        userMessage = `⚠️ Conflicto: ${message}`;
        break;
      case 422:
        userMessage = `⚠️ Datos inválidos: ${message}`;
        break;
      case 500:
        userMessage = '❌ Error del servidor. Por favor intenta más tarde.';
        break;
      case 503:
        userMessage = '⏳ Servicio no disponible. Por favor intenta más tarde.';
        break;
      default:
        userMessage = `❌ Error: ${message}`;
    }
    
    // Mostrar error en Snackbar si el callback está configurado
    if (showErrorCallback) {
      showErrorCallback(userMessage);
    }
    
    // Rechazar la promesa para que el catch funcione
    return Promise.reject(error);
  }
);

export default http;
