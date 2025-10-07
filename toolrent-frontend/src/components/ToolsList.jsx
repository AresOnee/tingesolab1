// src/components/ToolsList.jsx
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
import Chip from '@mui/material/Chip'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'

import toolService from '../services/tool.service'
import http from '../http-common' // <- usaremos http para el POST correcto

const STATUS_OPTIONS = ['Disponible', 'Prestada', 'Inactivo', 'En reparación']

const emptyForm = {
  name: '',
  category: '',
  replacementValue: '',
  stock: '',
  status: 'Disponible',
}

export default function ToolsList() {
  const { keycloak, initialized } = useKeycloak()
  const [tools, setTools] = useState([])
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
        // Listado sigue usando tu service original
        const rows = await toolService.getAll()
        if (alive) setTools(rows)
      } catch (e) {
        console.error(e)
        setError('No se pudieron cargar las herramientas.')
      } finally {
        setLoading(false)
      }
    })()
    return () => { alive = false }
  }, [initialized, canSee])

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const colorFor = (status = '') => {
    const s = status.toLowerCase()
    if (s.includes('disponible')) return 'success'
    if (s.includes('prestada')) return 'warning'
    if (s.includes('reparación')) return 'info'
    if (s.includes('inactivo')) return 'default'
    return 'default'
  }

  const onCreate = async () => {
    // Validación mínima
    if (!form.name.trim()) {
      setError('El nombre es obligatorio.')
      return
    }
    if (!form.category.trim()) {
      setError('La categoría es obligatoria.')
      return
    }

    try {
      setLoading(true)
      setError('')

      // ⚠️ Aquí está la clave: POST directo al endpoint correcto del backend
      await http.post('/api/v1/tools/', {
        name: form.name.trim(),
        category: form.category.trim(),
        replacementValue: Number(form.replacementValue || 0),
        stock: Number(form.stock || 0),
        status: form.status, // viene del Select
      })

      setForm(emptyForm)
      const rows = await toolService.getAll()
      setTools(rows)
    } catch (e) {
      console.error(e)
      // Mensajes más claros para errores comunes
      if (e?.response?.status === 400) {
        setError(e?.response?.data?.message || 'Datos inválidos (400).')
      } else if (e?.response?.status === 409) {
        setError(e?.response?.data?.message || 'Conflicto: el nombre ya existe (409).')
      } else {
        setError('No se pudo crear la herramienta.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>Herramientas</Typography>

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
            label="Categoría"
            name="category"
            value={form.category}
            onChange={onChange}
            fullWidth
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 2 }}>
          <TextField
            label="Reposición"
            name="replacementValue"
            value={form.replacementValue}
            onChange={onChange}
            type="number"
            fullWidth
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 1.5 }}>
          <TextField
            label="Stock"
            name="stock"
            value={form.stock}
            onChange={onChange}
            type="number"
            fullWidth
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 2.5 }}>
          <FormControl fullWidth>
            <InputLabel id="status-label">Estado</InputLabel>
            <Select
              labelId="status-label"
              label="Estado"
              name="status"
              value={form.status}
              onChange={onChange}
            >
              {STATUS_OPTIONS.map((opt) => (
                <MenuItem key={opt} value={opt}>{opt}</MenuItem>
              ))}
            </Select>
          </FormControl>
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
              <TableCell>Categoría</TableCell>
              <TableCell>Reposición</TableCell>
              <TableCell>Stock</TableCell>
              <TableCell>Estado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {tools.map((t) => (
              <TableRow key={t.id ?? `${t.name}-${t.category}`}>
                <TableCell>{t.id}</TableCell>
                <TableCell>{t.name}</TableCell>
                <TableCell>{t.category}</TableCell>
                <TableCell>{t.replacementValue}</TableCell>
                <TableCell>{t.stock}</TableCell>
                <TableCell>
                  <Chip size="small" color={colorFor(t.status)} label={t.status || '—'} />
                </TableCell>
              </TableRow>
            ))}
            {!loading && tools.length === 0 && (
              <TableRow><TableCell colSpan={6}>Sin datos</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}
