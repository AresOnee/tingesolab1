import { useEffect, useMemo, useState } from 'react'
import { useKeycloak } from '@react-keycloak/web'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import Grid from '@mui/material/Grid2'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Paper from '@mui/material/Paper'

import clientService from '../services/client.service'
import http from '../http-common'

const emptyForm = { name: '', rut: '', email: '', phone: '' }

export default function ClientsList() {
  const { keycloak, initialized } = useKeycloak()
  const [clients, setClients] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const canSee = useMemo(() => keycloak?.authenticated, [keycloak?.authenticated])

  useEffect(() => {
    if (!initialized || !canSee) return
    let alive = true

    ;(async () => {
      try {
        setLoading(true)
        setError('')
        const rows = await clientService.getAll() // GET /api/v1/clients/
        if (alive) setClients(rows)
      } catch (e) {
        console.error(e)
        setError('No se pudieron cargar los clientes.')
      } finally {
        setLoading(false)
      }
    })()

    return () => { alive = false }
  }, [initialized, canSee])

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const onCreate = async () => {
    // Validación mínima
    if (!form.name || !form.rut || !form.email || !form.phone) {
      setError('Completa nombre, RUT, email y teléfono.')
      return
    }

    try {
      setLoading(true)
      setError('')

       await http.post('/api/v1/clients', form)

      // Refrescamos
      setForm(emptyForm)
      const rows = await clientService.getAll()
      setClients(rows)
    } catch (e) {
      console.error(e)
      // Mensaje más claro si viene 409 desde el backend (RUT/email duplicados)
      const status = e?.response?.status
      const backendMsg =
        e?.response?.data?.message ||
        e?.response?.data?.error ||
        'No se pudo crear el cliente.'
      if (status === 409) {
        setError(backendMsg)
      } else if (status === 403) {
        setError('No tienes permisos para crear clientes.')
      } else {
        setError('No se pudo crear el cliente.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>Clientes</Typography>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 3 }}>
          <TextField
            label="Nombre"
            name="name"
            value={form.name}
            onChange={onChange}
            fullWidth
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 3 }}>
          <TextField
            label="RUT"
            name="rut"
            value={form.rut}
            onChange={onChange}
            fullWidth
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 3 }}>
          <TextField
            label="Email"
            name="email"
            value={form.email}
            onChange={onChange}
            fullWidth
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 2 }}>
          <TextField
            label="Teléfono"
            name="phone"
            value={form.phone}
            onChange={onChange}
            fullWidth
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 1 }}>
          <Button variant="contained" onClick={onCreate} disabled={loading}>
            CREAR
          </Button>
        </Grid>
      </Grid>

      {error && <Typography color="error" sx={{ mb: 1 }}>{error}</Typography>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Nombre</TableCell>
              <TableCell>RUT</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Teléfono</TableCell>
              <TableCell>Estado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {clients.map((c) => (
              <TableRow key={c.id ?? `${c.name}-${c.rut}`}>
                <TableCell>{c.id}</TableCell>
                <TableCell>{c.name}</TableCell>
                <TableCell>{c.rut}</TableCell>
                <TableCell>{c.email}</TableCell>
                <TableCell>{c.phone}</TableCell>
                <TableCell>{c.state}</TableCell>
              </TableRow>
            ))}
            {!loading && clients.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>Sin datos</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}
