// src/services/kardex.service.js
import http from '../http-common'

/**
 * Servicio para gestionar el Kardex (historial de movimientos)
 * Épica 5: Gestión de Kardex y Movimientos
 */

/**
 * RF5.2: Consultar el historial de movimientos de una herramienta específica
 * GET /api/v1/kardex/tool/{toolId}
 */
const getMovementsByTool = async (toolId) => {
  try {
    const { data } = await http.get(`/api/v1/kardex/tool/${toolId}`)
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener movimientos por herramienta:', error)
    throw error
  }
}

/**
 * RF5.3: Consultar movimientos en un rango de fechas
 * GET /api/v1/kardex/date-range?startDate={startDate}&endDate={endDate}
 */
const getMovementsByDateRange = async (startDate, endDate) => {
  try {
    const { data } = await http.get('/api/v1/kardex/date-range', {
      params: { 
        startDate: startDate,
        endDate: endDate 
      }
    })
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener movimientos por rango de fechas:', error)
    throw error
  }
}

/**
 * Obtener todos los movimientos del kardex
 * GET /api/v1/kardex
 */
const getAllMovements = async () => {
  try {
    const { data } = await http.get('/api/v1/kardex')
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener todos los movimientos:', error)
    throw error
  }
}

/**
 * Filtrar movimientos por tipo
 * GET /api/v1/kardex/type/{movementType}
 * Tipos válidos: INGRESO, PRESTAMO, DEVOLUCION, BAJA, REPARACION
 */
const getMovementsByType = async (movementType) => {
  try {
    const { data } = await http.get(`/api/v1/kardex/type/${movementType}`)
    return Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Error al obtener movimientos por tipo:', error)
    throw error
  }
}

const kardexService = {
  getMovementsByTool,
  getMovementsByDateRange,
  getAllMovements,
  getMovementsByType,
}

export default kardexService
