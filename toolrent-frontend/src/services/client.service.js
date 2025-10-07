import http from '../http-common'

const getAll = async () => {
  const { data } = await http.get('/api/v1/clients/')
  return Array.isArray(data) ? data : (data?.content ?? [])
}

const create = (payload) => http.post('/api/v1/clients/', payload)

export default { getAll, create }