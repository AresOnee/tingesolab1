import http from '../http-common'

const getAll = async () => {
  const { data } = await http.get('/api/v1/tools/') // <-- Cambio aquí
  return Array.isArray(data) ? data : (data?.content ?? [])
}

const create = (payload) => http.post('/api/v1/clients/', payload) // <-- Y aquí

export default { getAll, create }