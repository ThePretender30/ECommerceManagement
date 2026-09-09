import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks'
import { Loader } from './Common'

export function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Loader fullPage label="Checking your session…" />

  if (!isAuthenticated) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }

  return children
}

export function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Loader fullPage label="Checking your permissions…" />

  if (!isAuthenticated) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/admin/login?redirect=${redirect}`} replace />
  }

  if (!isAdmin) return <Navigate to="/admin/login" replace />

  return children
}

export function GuestRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) return <Loader fullPage />
  if (isAuthenticated) return <Navigate to="/" replace />

  return children
}
