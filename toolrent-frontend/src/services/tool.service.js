// src/services/tool.service.js
import http from '../http-common'

const getAll = async () => {
  const { data } = await http.get('/api/v1/tools/')
  return Array.isArray(data) ? data : (data?.content ?? [])
}

const create = (body) => {
  return http.post('/api/v1/tools/', body)
}

/**
 * RF1.2: Dar de baja herramientas (solo Admin)
 * PUT /api/v1/tools/{id}/decommission
 */
const decommission = async (toolId) => {
  const { data } = await http.put(`/api/v1/tools/${toolId}/decommission`)
  return data
}

const toolService = {
  getAll,
  create,
  decommission,
}

export default toolService