// src/services/report.service.js
import http from '../http-common'

/**
 * Servicio para consumir reportes del sistema
 * Épica 6: Reportes y consultas
 */

/**
 * RF6.1: Obtener préstamos activos (vigentes y atrasados)
 * GET /api/v1/reports/active-loans
 * 
 * @param {Object} params - Parámetros opcionales
 * @param {string} params.startDate - Fecha inicio (formato: YYYY-MM-DD)
 * @param {string} params.endDate - Fecha fin (formato: YYYY-MM-DD)
 * @returns {Promise<Array>} Lista de préstamos activos
 */
const getActiveLoans = async (params = {}) => {
  try {
    const { data } = await http.get('/api/v1/reports/active-loans', { params })
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener préstamos activos:', error)
    throw error
  }
}

/**
 * RF6.2: Obtener clientes con atrasos
 * GET /api/v1/reports/clients-with-overdues
 * 
 * @returns {Promise<Array>} Lista de clientes con préstamos atrasados
 */
const getClientsWithOverdues = async () => {
  try {
    const { data } = await http.get('/api/v1/reports/clients-with-overdues')
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener clientes con atrasos:', error)
    throw error
  }
}

/**
 * RF6.3: Obtener ranking de herramientas más prestadas
 * GET /api/v1/reports/most-loaned-tools
 * 
 * @param {Object} params - Parámetros opcionales
 * @param {string} params.startDate - Fecha inicio para filtrar (formato: YYYY-MM-DD)
 * @param {string} params.endDate - Fecha fin para filtrar (formato: YYYY-MM-DD)
 * @param {number} params.limit - Cantidad de resultados (default: 10)
 * @returns {Promise<Array>} Lista de herramientas con conteo de préstamos
 */
const getMostLoanedTools = async (params = {}) => {
  try {
    const { data } = await http.get('/api/v1/reports/most-loaned-tools', { params })
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener ranking de herramientas:', error)
    throw error
  }
}

const reportService = {
  getActiveLoans,
  getClientsWithOverdues,
  getMostLoanedTools,
}

export default reportService