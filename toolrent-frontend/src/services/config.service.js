// src/services/config.service.js
import http from '../http-common'

/**
 * Obtener todas las configuraciones
 * GET /api/v1/config
 */
const getAll = async () => {
  const { data } = await http.get('/api/v1/config')
  return Array.isArray(data) ? data : (data?.content ?? [])
}

/**
 * Obtener configuración por clave
 * GET /api/v1/config/key/{configKey}
 */
const getByKey = async (configKey) => {
  const { data } = await http.get(`/api/v1/config/key/${configKey}`)
  return data
}

/**
 * Obtener tarifa de arriendo diaria
 * GET /api/v1/config/tarifa-arriendo
 */
const getTarifaArriendo = async () => {
  const { data } = await http.get('/api/v1/config/tarifa-arriendo')
  return data.tarifaArriendoDiaria
}

/**
 * Obtener tarifa de multa diaria
 * GET /api/v1/config/tarifa-multa
 */
const getTarifaMulta = async () => {
  const { data } = await http.get('/api/v1/config/tarifa-multa')
  return data.tarifaMultaDiaria
}

/**
 * ✅ NUEVO: Obtener cargo por reparación
 * GET /api/v1/config/cargo-reparacion
 * 
 * Épica 2 - RN #16: Retorna el cargo configurable para herramientas con daños leves
 */
const getCargoReparacion = async () => {
  const { data } = await http.get('/api/v1/config/cargo-reparacion')
  return data.cargoReparacion
}

/**
 * Actualizar configuración por ID (solo Admin)
 * PUT /api/v1/config/{id}
 * Body: { value: 5000.0 }
 */
const updateById = async (id, value) => {
  const { data } = await http.put(`/api/v1/config/${id}`, { value })
  return data
}

/**
 * Actualizar tarifa de arriendo (solo Admin)
 * PUT /api/v1/config/tarifa-arriendo
 * Body: { value: 5000.0 }
 */
const updateTarifaArriendo = async (value) => {
  const { data } = await http.put('/api/v1/config/tarifa-arriendo', { value })
  return data
}

/**
 * Actualizar tarifa de multa (solo Admin)
 * PUT /api/v1/config/tarifa-multa
 * Body: { value: 2000.0 }
 */
const updateTarifaMulta = async (value) => {
  const { data } = await http.put('/api/v1/config/tarifa-multa', { value })
  return data
}

/**
 * ✅ NUEVO: Actualizar cargo por reparación (solo Admin)
 * PUT /api/v1/config/cargo-reparacion
 * Body: { value: 10000.0 }
 * 
 * Épica 2 - RN #16: Permite configurar el cargo por reparación de daños leves
 */
const updateCargoReparacion = async (value) => {
  const { data } = await http.put('/api/v1/config/cargo-reparacion', { value })
  return data
}

const configService = {
  getAll,
  getByKey,
  getTarifaArriendo,
  getTarifaMulta,
  getCargoReparacion,      // ✅ NUEVO
  updateById,
  updateTarifaArriendo,
  updateTarifaMulta,
  updateCargoReparacion,   // ✅ NUEVO
}

export default configService