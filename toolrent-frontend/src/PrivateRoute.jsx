// src/PrivateRoute.jsx
import { Navigate } from 'react-router-dom'

function PrivateRoute({ element, children, roles = [] }) {
  const kc = window.keycloak

  if (!kc) return <div style={{ padding: 24 }}>Cargando…</div>

  if (!kc.authenticated) {
    kc.login()
    return null
  }

  const realmRoles = kc.tokenParsed?.realm_access?.roles || []
  const clientRolesObj = kc.tokenParsed?.resource_access || {}
  const clientRoles = Object.values(clientRolesObj).flatMap(r => r?.roles || [])

  const allUpper = new Set([...realmRoles, ...clientRoles].map(r => String(r).toUpperCase()))
  const reqUpper = roles.map(r => String(r).toUpperCase())
  const allowed = reqUpper.length === 0 || reqUpper.some(r => allUpper.has(r))

  const content = element ?? children ?? null
  return allowed ? content : <Navigate to="/unauthorized" replace />
}

export default PrivateRoute
