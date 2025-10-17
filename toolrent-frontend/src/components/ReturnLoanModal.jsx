// src/components/ReturnLoanModal.jsx
import { useState, useEffect } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Radio from '@mui/material/Radio'
import RadioGroup from '@mui/material/RadioGroup'
import FormControlLabel from '@mui/material/FormControlLabel'
import FormControl from '@mui/material/FormControl'
import FormLabel from '@mui/material/FormLabel'
import Divider from '@mui/material/Divider'
import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import BuildIcon from '@mui/icons-material/Build'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'

import configService from '../services/config.service'

/**
 * Modal Profesional para Devolución de Herramientas
 * 
 * ✅ CORRECCIÓN CRÍTICA: Parsing correcto de fechas en zona horaria local
 */
export default function ReturnLoanModal({
  open,
  onClose,
  loan,
  onConfirm,
  clientName,
  toolName,
}) {
  const [damageType, setDamageType] = useState('sin_danos')
  const [loading, setLoading] = useState(false)
  const [loadingConfig, setLoadingConfig] = useState(true)
  
  // Configuraciones desde el sistema
  const [tarifaMulta, setTarifaMulta] = useState(0)
  const [cargoReparacion, setCargoReparacion] = useState(0)
  const [valorReposicion, setValorReposicion] = useState(0)

  // Cargar configuraciones al abrir el modal
  useEffect(() => {
    if (open && loan) {
      loadConfigurations()
    }
  }, [open, loan])

  const loadConfigurations = async () => {
    try {
      setLoadingConfig(true)
      const [multa, reparacion] = await Promise.all([
        configService.getTarifaMulta(),
        configService.getCargoReparacion(),
      ])
      
      setTarifaMulta(multa || 0)
      setCargoReparacion(reparacion || 0)
      
      // Obtener valor de reposición de la herramienta desde el préstamo
      const reposicion = loan?.tool?.replacementValue || 0
      setValorReposicion(reposicion)
      
    } catch (error) {
      console.error('Error al cargar configuraciones:', error)
    } finally {
      setLoadingConfig(false)
    }
  }

  /**
   * ✅ CORRECCIÓN CRÍTICA: Parser de fecha local
   * 
   * Problema: new Date("2025-10-16") interpreta como UTC medianoche,
   * pero en Chile (UTC-3) esto se convierte a 2025-10-15 21:00:00 local
   * 
   * Solución: Parsear la fecha explícitamente en zona horaria local
   */
  const parseLocalDate = (dateString) => {
    if (!dateString) return null
    
    // Separar componentes de la fecha
    const [year, month, day] = dateString.split('-').map(Number)
    
    // Crear fecha en zona horaria LOCAL (month - 1 porque enero = 0)
    const localDate = new Date(year, month - 1, day)
    localDate.setHours(0, 0, 0, 0)
    
    return localDate
  }

  /**
   * ✅ CORRECCIÓN CRÍTICA: Calcular días de atraso correctamente
   * 
   * Cambios:
   * 1. Usar parseLocalDate() para interpretar fecha en zona horaria local
   * 2. Usar Math.floor() para contar solo días completos
   */
  const calcularDiasAtraso = () => {
    if (!loan?.dueDate) return 0
    
    // ✅ Parsear fecha límite en zona horaria local
    const fechaLimite = parseLocalDate(loan.dueDate)
    if (!fechaLimite) return 0
    
    // Obtener fecha actual en zona horaria local
    const hoy = new Date()
    hoy.setHours(0, 0, 0, 0)
    
    // Solo hay atraso si hoy > fechaLimite (no igual)
    if (hoy <= fechaLimite) return 0
    
    // Calcular diferencia en días completos
    const diffTime = hoy - fechaLimite
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
    
    return diffDays
  }

  const diasAtraso = calcularDiasAtraso()
  const multaAtraso = diasAtraso * tarifaMulta

  // Calcular cargo por daño según tipo seleccionado
  const calcularCargoDano = () => {
    switch (damageType) {
      case 'sin_danos':
        return 0
      case 'danos_leves':
        return cargoReparacion
      case 'danos_graves':
        return valorReposicion
      default:
        return 0
    }
  }

  const cargoDano = calcularCargoDano()
  const totalCobrar = multaAtraso + cargoDano

  // Formatear moneda
  const formatCurrency = (value) => {
    if (value === null || value === undefined) return '$0'
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(value)
  }

  const handleConfirm = async () => {
    if (!loan) return

    let isDamaged = false
    let isIrreparable = false

    if (damageType === 'danos_leves') {
      isDamaged = true
      isIrreparable = false
    } else if (damageType === 'danos_graves') {
      isDamaged = true
      isIrreparable = true
    }

    setLoading(true)
    try {
      await onConfirm(loan.id, isDamaged, isIrreparable)
      handleClose()
    } catch (error) {
      console.error('Error en devolución:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setDamageType('sin_danos')
    setLoading(false)
    onClose()
  }

  if (!loan) return null

  return (
    <Dialog 
      open={open} 
      onClose={loading ? null : handleClose}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white', pb: 2 }}>
        <Box display="flex" alignItems="center" gap={1}>
          <BuildIcon />
          <Typography variant="h6" component="span">
            Devolver Herramienta
          </Typography>
        </Box>
      </DialogTitle>

      <DialogContent sx={{ pt: 3 }}>
        {loadingConfig ? (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            {/* Información del Préstamo */}
            <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>Cliente:</strong> {clientName || 'Desconocido'}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>Herramienta:</strong> {toolName || 'Desconocida'}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>Fecha Límite:</strong> {loan.dueDate || '-'}
              </Typography>
              {diasAtraso > 0 && (
                <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ mt: 1 }}>
                  <strong>Préstamo Atrasado:</strong> {diasAtraso} día{diasAtraso !== 1 ? 's' : ''}
                </Alert>
              )}
            </Box>

            <Divider sx={{ my: 2 }} />

            {/* Selección de Estado de la Herramienta */}
            <FormControl component="fieldset" fullWidth sx={{ mb: 3 }}>
              <FormLabel component="legend" sx={{ mb: 1, fontWeight: 600 }}>
                Estado de la Herramienta al Devolverla:
              </FormLabel>
              <RadioGroup
                value={damageType}
                onChange={(e) => setDamageType(e.target.value)}
              >
                <FormControlLabel
                  value="sin_danos"
                  control={<Radio />}
                  label={
                    <Box display="flex" alignItems="center" gap={1}>
                      <CheckCircleIcon color="success" fontSize="small" />
                      <Typography>Sin daños (estado normal)</Typography>
                    </Box>
                  }
                  sx={{ 
                    mb: 1, 
                    p: 1.5, 
                    border: '1px solid',
                    borderColor: damageType === 'sin_danos' ? 'success.main' : 'grey.300',
                    borderRadius: 1,
                    bgcolor: damageType === 'sin_danos' ? 'success.50' : 'transparent'
                  }}
                />
                <FormControlLabel
                  value="danos_leves"
                  control={<Radio />}
                  label={
                    <Box>
                      <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                        <BuildIcon color="warning" fontSize="small" />
                        <Typography>Daños leves (reparable)</Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary" sx={{ ml: 3 }}>
                        Cargo adicional: {formatCurrency(cargoReparacion)}
                      </Typography>
                    </Box>
                  }
                  sx={{ 
                    mb: 1, 
                    p: 1.5, 
                    border: '1px solid',
                    borderColor: damageType === 'danos_leves' ? 'warning.main' : 'grey.300',
                    borderRadius: 1,
                    bgcolor: damageType === 'danos_leves' ? 'warning.50' : 'transparent'
                  }}
                />
                <FormControlLabel
                  value="danos_graves"
                  control={<Radio />}
                  label={
                    <Box>
                      <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                        <DeleteForeverIcon color="error" fontSize="small" />
                        <Typography>Daños graves (irreparable)</Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary" sx={{ ml: 3 }}>
                        Valor de reposición: {formatCurrency(valorReposicion)}
                      </Typography>
                    </Box>
                  }
                  sx={{ 
                    p: 1.5, 
                    border: '1px solid',
                    borderColor: damageType === 'danos_graves' ? 'error.main' : 'grey.300',
                    borderRadius: 1,
                    bgcolor: damageType === 'danos_graves' ? 'error.50' : 'transparent'
                  }}
                />
              </RadioGroup>
            </FormControl>

            <Divider sx={{ my: 2 }} />

            {/* Resumen de Cargos */}
            <Box 
              sx={{ 
                p: 2, 
                bgcolor: 'primary.50', 
                borderRadius: 1,
                border: '2px solid',
                borderColor: 'primary.main'
              }}
            >
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                RESUMEN DE CARGOS:
              </Typography>
              
              <Box display="flex" justifyContent="space-between" mb={1}>
                <Typography variant="body2">
                  • Multa por atraso ({diasAtraso} día{diasAtraso !== 1 ? 's' : ''}):
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {formatCurrency(multaAtraso)}
                </Typography>
              </Box>

              <Box display="flex" justifyContent="space-between" mb={1}>
                <Typography variant="body2">
                  • Cargo por daño:
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {formatCurrency(cargoDano)}
                </Typography>
              </Box>

              <Divider sx={{ my: 1 }} />

              <Box display="flex" justifyContent="space-between">
                <Typography variant="h6" fontWeight={700}>
                  TOTAL A COBRAR:
                </Typography>
                <Typography 
                  variant="h6" 
                  fontWeight={700}
                  color={totalCobrar > 0 ? 'error.main' : 'success.main'}
                >
                  {formatCurrency(totalCobrar)}
                </Typography>
              </Box>
            </Box>

            {damageType === 'danos_graves' && (
              <Alert severity="error" sx={{ mt: 2 }}>
                <strong>ADVERTENCIA:</strong> La herramienta será dada de baja permanentemente del inventario.
              </Alert>
            )}
          </>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button 
          onClick={handleClose} 
          disabled={loading}
          variant="outlined"
        >
          Cancelar
        </Button>
        <Button 
          onClick={handleConfirm}
          disabled={loading || loadingConfig}
          variant="contained"
          color="primary"
          startIcon={loading && <CircularProgress size={20} />}
        >
          {loading ? 'Procesando...' : 'Confirmar Devolución'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}