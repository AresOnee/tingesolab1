// src/constants/index.js

/**
 * Constantes centralizadas del sistema ToolRent
 * Evita valores mágicos y facilita mantenimiento
 */

// ==================== ESTADOS DE HERRAMIENTAS ====================
export const TOOL_STATUS = {
  AVAILABLE: 'Disponible',
  LOANED: 'Prestada',
  IN_USE: 'En uso',
  IN_REPAIR: 'En reparación',
  DECOMMISSIONED: 'Dada de baja'
};

export const TOOL_STATUS_COLORS = {
  [TOOL_STATUS.AVAILABLE]: 'success',
  [TOOL_STATUS.LOANED]: 'warning',
  [TOOL_STATUS.IN_USE]: 'warning',
  [TOOL_STATUS.IN_REPAIR]: 'info',
  [TOOL_STATUS.DECOMMISSIONED]: 'error'
};

export const TOOL_STATUS_ICONS = {
  [TOOL_STATUS.AVAILABLE]: '✅',
  [TOOL_STATUS.LOANED]: '📦',
  [TOOL_STATUS.IN_USE]: '🔧',
  [TOOL_STATUS.IN_REPAIR]: '🔨',
  [TOOL_STATUS.DECOMMISSIONED]: '❌'
};

// ==================== ESTADOS DE CLIENTES ====================
export const CLIENT_STATUS = {
  ACTIVE: 'Activo',
  RESTRICTED: 'Restringido'
};

export const CLIENT_STATUS_COLORS = {
  [CLIENT_STATUS.ACTIVE]: 'success',
  [CLIENT_STATUS.RESTRICTED]: 'error'
};

export const CLIENT_STATUS_DESCRIPTIONS = {
  [CLIENT_STATUS.ACTIVE]: 'Puede solicitar préstamos',
  [CLIENT_STATUS.RESTRICTED]: 'No puede solicitar préstamos hasta regularizar deudas'
};

// ==================== TIPOS DE MOVIMIENTO KARDEX ====================
export const MOVEMENT_TYPES = {
  LOAN: 'Prestamo',
  RETURN: 'Devolucion',
  PURCHASE: 'Compra',
  DECOMMISSION: 'Baja',
  ADJUSTMENT: 'Ajuste'
};

export const MOVEMENT_TYPE_COLORS = {
  [MOVEMENT_TYPES.LOAN]: 'warning',
  [MOVEMENT_TYPES.RETURN]: 'success',
  [MOVEMENT_TYPES.PURCHASE]: 'info',
  [MOVEMENT_TYPES.DECOMMISSION]: 'error',
  [MOVEMENT_TYPES.ADJUSTMENT]: 'default'
};

export const MOVEMENT_TYPE_ICONS = {
  [MOVEMENT_TYPES.LOAN]: '📤',
  [MOVEMENT_TYPES.RETURN]: '📥',
  [MOVEMENT_TYPES.PURCHASE]: '🛒',
  [MOVEMENT_TYPES.DECOMMISSION]: '🗑️',
  [MOVEMENT_TYPES.ADJUSTMENT]: '⚖️'
};

// ==================== CONFIGURACIONES ====================
export const CONFIG_KEYS = {
  DAILY_FEE: 'MULTA_DIARIA',
  REPAIR_CHARGE_MINOR: 'CARGO_REPARACION',
  REPAIR_CHARGE_MAJOR: 'CARGO_BAJA',
  MAX_ACTIVE_LOANS: 'MAX_PRESTAMOS_ACTIVOS'
};

export const CONFIG_LABELS = {
  [CONFIG_KEYS.DAILY_FEE]: 'Multa Diaria por Atraso',
  [CONFIG_KEYS.REPAIR_CHARGE_MINOR]: 'Cargo por Reparación Menor',
  [CONFIG_KEYS.REPAIR_CHARGE_MAJOR]: 'Cargo por Daño Irreparable',
  [CONFIG_KEYS.MAX_ACTIVE_LOANS]: 'Máximo de Préstamos Activos'
};

export const CONFIG_DESCRIPTIONS = {
  [CONFIG_KEYS.DAILY_FEE]: 'Monto cobrado por cada día de atraso en la devolución',
  [CONFIG_KEYS.REPAIR_CHARGE_MINOR]: 'Cargo aplicado cuando la herramienta requiere reparación menor',
  [CONFIG_KEYS.REPAIR_CHARGE_MAJOR]: 'Cargo aplicado cuando la herramienta tiene daño irreparable',
  [CONFIG_KEYS.MAX_ACTIVE_LOANS]: 'Cantidad máxima de préstamos activos por cliente'
};

// ==================== ROLES DE USUARIO ====================
export const USER_ROLES = {
  ADMIN: 'ADMIN',
  USER: 'USER'
};

export const USER_ROLE_LABELS = {
  [USER_ROLES.ADMIN]: 'Administrador',
  [USER_ROLES.USER]: 'Usuario'
};

// ==================== VALIDACIONES ====================
export const VALIDATION_RULES = {
  RUT: {
    PATTERN: /^\d{7,8}-[\dkK]$/,
    MIN_LENGTH: 9,
    MAX_LENGTH: 10
  },
  EMAIL: {
    PATTERN: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  },
  PHONE: {
    PATTERN: /^(\+56)?[2-9]\d{8}$/,
    CHILE_FORMAT: /^(\+56)?9\d{8}$/
  },
  MAX_LOAN_DAYS: 30,
  MIN_STOCK: 0,
  MAX_STOCK: 999,
  MIN_REPLACEMENT_VALUE: 1000,
  MAX_ACTIVE_LOANS_PER_CLIENT: 5
};

// ==================== MENSAJES DE ERROR ====================
export const ERROR_MESSAGES = {
  // Validaciones
  REQUIRED_FIELD: 'Este campo es obligatorio',
  INVALID_RUT: 'RUT inválido. Formato: 12345678-9',
  INVALID_EMAIL: 'Email inválido. Formato: usuario@dominio.cl',
  INVALID_PHONE: 'Teléfono inválido. Formato: +56912345678',
  INVALID_DATE: 'Fecha inválida',
  DATE_IN_PAST: 'La fecha no puede ser anterior a hoy',
  
  // Cliente
  CLIENT_NOT_FOUND: 'Cliente no encontrado',
  CLIENT_RESTRICTED: 'Cliente restringido. No puede solicitar préstamos',
  CLIENT_HAS_OVERDUES: 'Cliente tiene préstamos vencidos',
  CLIENT_HAS_FINES: 'Cliente tiene multas pendientes',
  CLIENT_MAX_LOANS: 'Cliente alcanzó el límite de préstamos activos',
  
  // Herramienta
  TOOL_NOT_FOUND: 'Herramienta no encontrada',
  TOOL_NOT_AVAILABLE: 'Herramienta no disponible',
  TOOL_NO_STOCK: 'Herramienta sin stock disponible',
  TOOL_ALREADY_LOANED: 'Cliente ya tiene un préstamo activo de esta herramienta',
  
  // Préstamo
  LOAN_NOT_FOUND: 'Préstamo no encontrado',
  LOAN_ALREADY_RETURNED: 'Préstamo ya fue devuelto',
  
  // General
  NETWORK_ERROR: 'Error de conexión. Verifica que el servidor esté funcionando',
  UNAUTHORIZED: 'No autorizado. Por favor inicia sesión nuevamente',
  FORBIDDEN: 'No tienes permisos para realizar esta acción',
  SERVER_ERROR: 'Error del servidor. Por favor intenta más tarde'
};

// ==================== MENSAJES DE ÉXITO ====================
export const SUCCESS_MESSAGES = {
  // Cliente
  CLIENT_CREATED: 'Cliente creado exitosamente',
  CLIENT_UPDATED: 'Cliente actualizado exitosamente',
  CLIENT_STATE_CHANGED: 'Estado del cliente cambiado exitosamente',
  
  // Herramienta
  TOOL_CREATED: 'Herramienta creada exitosamente',
  TOOL_UPDATED: 'Herramienta actualizada exitosamente',
  TOOL_DECOMMISSIONED: 'Herramienta dada de baja exitosamente',
  
  // Préstamo
  LOAN_CREATED: 'Préstamo creado exitosamente',
  LOAN_RETURNED: 'Herramienta devuelta exitosamente',
  
  // Configuración
  CONFIG_UPDATED: 'Configuración actualizada exitosamente'
};

// ==================== FORMATO DE MONEDA ====================
export const CURRENCY_FORMAT = {
  LOCALE: 'es-CL',
  CURRENCY: 'CLP',
  OPTIONS: {
    style: 'currency',
    currency: 'CLP',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }
};

/**
 * Formatea un número como moneda chilena
 * @param {number} value - Valor a formatear
 * @returns {string} Valor formateado (ej: "$50.000")
 */
export const formatCurrency = (value) => {
  return new Intl.NumberFormat(CURRENCY_FORMAT.LOCALE, CURRENCY_FORMAT.OPTIONS)
    .format(value);
};

// ==================== FORMATO DE FECHA ====================
export const DATE_FORMAT = {
  LOCALE: 'es-CL',
  SHORT: 'dd/MM/yyyy',
  LONG: 'dd MMMM yyyy',
  WITH_TIME: 'dd/MM/yyyy HH:mm'
};

/**
 * Formatea una fecha
 * @param {string|Date} date - Fecha a formatear
 * @returns {string} Fecha formateada (ej: "17/10/2025")
 */
export const formatDate = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return d.toLocaleDateString(DATE_FORMAT.LOCALE);
};

/**
 * Formatea una fecha con hora
 * @param {string|Date} date - Fecha a formatear
 * @returns {string} Fecha formateada con hora (ej: "17/10/2025 14:30")
 */
export const formatDateTime = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return d.toLocaleString(DATE_FORMAT.LOCALE);
};

// ==================== UTILIDADES ====================

/**
 * Obtiene el color según el estado de la herramienta
 * @param {string} status - Estado de la herramienta
 * @returns {string} Color de Material-UI
 */
export const getToolStatusColor = (status) => {
  return TOOL_STATUS_COLORS[status] || 'default';
};

/**
 * Obtiene el color según el estado del cliente
 * @param {string} status - Estado del cliente
 * @returns {string} Color de Material-UI
 */
export const getClientStatusColor = (status) => {
  return CLIENT_STATUS_COLORS[status] || 'default';
};

/**
 * Obtiene el color según el tipo de movimiento
 * @param {string} type - Tipo de movimiento
 * @returns {string} Color de Material-UI
 */
export const getMovementTypeColor = (type) => {
  return MOVEMENT_TYPE_COLORS[type] || 'default';
};

export default {
  TOOL_STATUS,
  TOOL_STATUS_COLORS,
  CLIENT_STATUS,
  CLIENT_STATUS_COLORS,
  MOVEMENT_TYPES,
  MOVEMENT_TYPE_COLORS,
  CONFIG_KEYS,
  CONFIG_LABELS,
  USER_ROLES,
  VALIDATION_RULES,
  ERROR_MESSAGES,
  SUCCESS_MESSAGES,
  formatCurrency,
  formatDate,
  formatDateTime,
  getToolStatusColor,
  getClientStatusColor,
  getMovementTypeColor
};
