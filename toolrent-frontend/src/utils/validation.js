// src/utils/validation.js

/**
 * Utilidades de validación para formularios
 * Incluye validaciones robustas para RUT, email, teléfono, etc.
 */

// ==================== VALIDACIÓN DE RUT ====================

/**
 * Limpia un RUT eliminando puntos y dejando solo números y guión
 * @param {string} rut - RUT a limpiar
 * @returns {string} RUT limpio (ej: "12345678-9")
 */
export const cleanRut = (rut) => {
  return rut
    .replace(/\./g, '')
    .replace(/\-/g, '')
    .trim()
    .toUpperCase();
};

/**
 * Formatea un RUT con puntos y guión
 * @param {string} rut - RUT a formatear
 * @returns {string} RUT formateado (ej: "12.345.678-9")
 */
export const formatRut = (rut) => {
  const cleaned = cleanRut(rut);
  if (cleaned.length < 2) return cleaned;
  
  const body = cleaned.slice(0, -1);
  const dv = cleaned.slice(-1);
  
  const formattedBody = body.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${formattedBody}-${dv}`;
};

/**
 * Calcula el dígito verificador de un RUT
 * @param {string} rut - Cuerpo del RUT sin DV
 * @returns {string} Dígito verificador ('0'-'9' o 'K')
 */
export const calculateRutDV = (rut) => {
  let sum = 0;
  let multiplier = 2;
  
  // Recorrer el RUT de derecha a izquierda
  for (let i = rut.length - 1; i >= 0; i--) {
    sum += parseInt(rut[i]) * multiplier;
    multiplier = multiplier === 7 ? 2 : multiplier + 1;
  }
  
  const remainder = sum % 11;
  const dv = 11 - remainder;
  
  if (dv === 11) return '0';
  if (dv === 10) return 'K';
  return dv.toString();
};

/**
 * Valida un RUT chileno completo (con dígito verificador)
 * @param {string} rut - RUT a validar
 * @returns {boolean} true si el RUT es válido
 */
export const isValidRut = (rut) => {
  if (!rut) return false;
  
  // Limpiar y separar cuerpo y DV
  const cleaned = cleanRut(rut);
  
  // Validar formato básico
  if (!/^\d{7,8}[0-9K]$/.test(cleaned)) return false;
  
  const body = cleaned.slice(0, -1);
  const dv = cleaned.slice(-1);
  
  // Calcular DV esperado y comparar
  const expectedDV = calculateRutDV(body);
  return dv === expectedDV;
};

/**
 * Obtiene mensaje de error para RUT inválido
 * @param {string} rut - RUT a validar
 * @returns {string|null} Mensaje de error o null si es válido
 */
export const getRutError = (rut) => {
  if (!rut) return 'RUT es obligatorio';
  if (rut.length < 9) return 'RUT muy corto';
  if (!isValidRut(rut)) return 'RUT inválido o dígito verificador incorrecto';
  return null;
};

// ==================== VALIDACIÓN DE EMAIL ====================

/**
 * Valida un email
 * @param {string} email - Email a validar
 * @returns {boolean} true si el email es válido
 */
export const isValidEmail = (email) => {
  if (!email) return false;
  
  // Regex robusto para email
  const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return regex.test(email.trim());
};

/**
 * Obtiene mensaje de error para email inválido
 * @param {string} email - Email a validar
 * @returns {string|null} Mensaje de error o null si es válido
 */
export const getEmailError = (email) => {
  if (!email) return 'Email es obligatorio';
  if (!isValidEmail(email)) return 'Email inválido. Formato: usuario@dominio.cl';
  return null;
};

// ==================== VALIDACIÓN DE TELÉFONO ====================

/**
 * Limpia un número de teléfono
 * @param {string} phone - Teléfono a limpiar
 * @returns {string} Teléfono limpio
 */
export const cleanPhone = (phone) => {
  return phone.replace(/\s/g, '').replace(/\-/g, '');
};

/**
 * Formatea un teléfono chileno
 * @param {string} phone - Teléfono a formatear
 * @returns {string} Teléfono formateado (ej: "+56 9 1234 5678")
 */
export const formatPhone = (phone) => {
  const cleaned = cleanPhone(phone);
  
  // Formato: +56 9 1234 5678
  if (cleaned.startsWith('+56')) {
    const number = cleaned.substring(3);
    return `+56 ${number[0]} ${number.substring(1, 5)} ${number.substring(5)}`;
  }
  
  // Formato: 9 1234 5678
  if (cleaned.startsWith('9')) {
    return `${cleaned[0]} ${cleaned.substring(1, 5)} ${cleaned.substring(5)}`;
  }
  
  return cleaned;
};

/**
 * Valida un teléfono chileno
 * @param {string} phone - Teléfono a validar
 * @returns {boolean} true si el teléfono es válido
 */
export const isValidPhone = (phone) => {
  if (!phone) return false;
  
  const cleaned = cleanPhone(phone);
  
  // Celular chileno: +569XXXXXXXX o 9XXXXXXXX
  const cellularRegex = /^(\+56)?9\d{8}$/;
  
  // Teléfono fijo: +56[2-9]XXXXXXXX o [2-9]XXXXXXXX
  const landlineRegex = /^(\+56)?[2-9]\d{8}$/;
  
  return cellularRegex.test(cleaned) || landlineRegex.test(cleaned);
};

/**
 * Obtiene mensaje de error para teléfono inválido
 * @param {string} phone - Teléfono a validar
 * @returns {string|null} Mensaje de error o null si es válido
 */
export const getPhoneError = (phone) => {
  if (!phone) return 'Teléfono es obligatorio';
  if (!isValidPhone(phone)) return 'Teléfono inválido. Formato: +56912345678';
  return null;
};

// ==================== VALIDACIÓN DE FECHAS ====================

/**
 * Valida que una fecha no sea anterior a hoy
 * @param {string|Date} date - Fecha a validar
 * @returns {boolean} true si la fecha es válida
 */
export const isValidFutureDate = (date) => {
  if (!date) return false;
  
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  const selectedDate = new Date(date);
  selectedDate.setHours(0, 0, 0, 0);
  
  return selectedDate >= today;
};

/**
 * Valida que una fecha esté dentro del rango máximo de días
 * @param {string|Date} date - Fecha a validar
 * @param {number} maxDays - Máximo de días permitidos
 * @returns {boolean} true si la fecha está dentro del rango
 */
export const isWithinMaxDays = (date, maxDays = 30) => {
  if (!date) return false;
  
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  const selectedDate = new Date(date);
  selectedDate.setHours(0, 0, 0, 0);
  
  const maxDate = new Date(today);
  maxDate.setDate(maxDate.getDate() + maxDays);
  
  return selectedDate >= today && selectedDate <= maxDate;
};

/**
 * Calcula días de diferencia entre dos fechas
 * @param {string|Date} startDate - Fecha inicial
 * @param {string|Date} endDate - Fecha final
 * @returns {number} Días de diferencia
 */
export const calculateDaysDifference = (startDate, endDate) => {
  const start = new Date(startDate);
  const end = new Date(endDate);
  
  const diffTime = Math.abs(end - start);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  return diffDays;
};

/**
 * Obtiene mensaje de error para fecha inválida
 * @param {string|Date} date - Fecha a validar
 * @param {number} maxDays - Máximo de días permitidos
 * @returns {string|null} Mensaje de error o null si es válida
 */
export const getDateError = (date, maxDays = 30) => {
  if (!date) return 'Fecha es obligatoria';
  
  if (!isValidFutureDate(date)) {
    return 'La fecha no puede ser anterior a hoy';
  }
  
  if (!isWithinMaxDays(date, maxDays)) {
    return `La fecha no puede ser mayor a ${maxDays} días desde hoy`;
  }
  
  return null;
};

// ==================== VALIDACIÓN DE NÚMEROS ====================

/**
 * Valida que un valor esté dentro de un rango
 * @param {number} value - Valor a validar
 * @param {number} min - Valor mínimo
 * @param {number} max - Valor máximo
 * @returns {boolean} true si el valor está en el rango
 */
export const isWithinRange = (value, min, max) => {
  const num = Number(value);
  return !isNaN(num) && num >= min && num <= max;
};

/**
 * Valida que un valor sea un número positivo
 * @param {number} value - Valor a validar
 * @returns {boolean} true si es positivo
 */
export const isPositiveNumber = (value) => {
  const num = Number(value);
  return !isNaN(num) && num > 0;
};

/**
 * Obtiene mensaje de error para valor numérico inválido
 * @param {number} value - Valor a validar
 * @param {number} min - Valor mínimo
 * @param {number} max - Valor máximo
 * @returns {string|null} Mensaje de error o null si es válido
 */
export const getNumberError = (value, min = 0, max = Infinity) => {
  if (value === '' || value === null || value === undefined) {
    return 'Este campo es obligatorio';
  }
  
  const num = Number(value);
  
  if (isNaN(num)) {
    return 'Debe ser un número válido';
  }
  
  if (num < min) {
    return `El valor mínimo es ${min}`;
  }
  
  if (num > max) {
    return `El valor máximo es ${max}`;
  }
  
  return null;
};

// ==================== VALIDACIÓN DE FORMULARIOS ====================

/**
 * Valida un formulario completo
 * @param {Object} values - Valores del formulario
 * @param {Object} rules - Reglas de validación
 * @returns {Object} Objeto con errores { field: 'error message' }
 * 
 * @example
 * const rules = {
 *   rut: (value) => getRutError(value),
 *   email: (value) => getEmailError(value)
 * };
 * 
 * const errors = validateForm(formValues, rules);
 * if (Object.keys(errors).length > 0) {
 *   // Hay errores
 * }
 */
export const validateForm = (values, rules) => {
  const errors = {};
  
  Object.keys(rules).forEach(field => {
    const validationFn = rules[field];
    const error = validationFn(values[field]);
    
    if (error) {
      errors[field] = error;
    }
  });
  
  return errors;
};

/**
 * Verifica si un formulario tiene errores
 * @param {Object} errors - Objeto con errores
 * @returns {boolean} true si hay errores
 */
export const hasErrors = (errors) => {
  return Object.keys(errors).length > 0;
};

// ==================== EXPORTACIONES ====================

export default {
  // RUT
  cleanRut,
  formatRut,
  calculateRutDV,
  isValidRut,
  getRutError,
  
  // Email
  isValidEmail,
  getEmailError,
  
  // Teléfono
  cleanPhone,
  formatPhone,
  isValidPhone,
  getPhoneError,
  
  // Fechas
  isValidFutureDate,
  isWithinMaxDays,
  calculateDaysDifference,
  getDateError,
  
  // Números
  isWithinRange,
  isPositiveNumber,
  getNumberError,
  
  // Formularios
  validateForm,
  hasErrors
};
