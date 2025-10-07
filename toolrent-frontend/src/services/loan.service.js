import http from '../http-common'

const getAll = async () => {
  const { data } = await http.get('/api/v1/loans/')
  return Array.isArray(data) ? data : (data?.content ?? [])
}

const create = (payload) => http.post('/api/v1/loans/create', null, { params: payload })

export default { getAll, create } // <-- No olvides agregar "getAll" aquí