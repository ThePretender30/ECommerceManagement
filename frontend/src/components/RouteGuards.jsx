import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks'
import { Loader } from './Common'

/**
 * Route guards.
 *
 * These are a user-experience convenience, NOT the security boundary. Every
 * protected endpoint is enforced independently by Spring Security, so removing
 * these guards in devtools would reveal empty pages, not other people's data.
 * Their real job is to send someone to the login screen instead of showing
 * them a page that will immediately fail with a 401.
 */

/** Requires a signed-in user. */
export function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()

  // Wait for the initial session check, otherwise a refresh on a protected
  // page would bounce the user to login before the token is validated.
  if (loading) return <Loader fullPage label="Checking your session…" />

  if (!isAuthenticated) {
    // Remember where they were headed so login can return them there.
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }

  return children
}

/** Requires a signed-in user who also holds ROLE_ADMIN. */
export function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Loader fullPage label="Checking your permissions…" />

  if (!isAuthenticated) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }

  // Signed in but not an admin: this is a 403, not a login problem, so send
  // them home rather than to a login form they have already completed.
  if (!isAdmin) return <Navigate to="/" replace />

  return children
}

/** Keeps already-signed-in users away from the login and register pages. */
export function GuestRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) return <Loader fullPage />
  if (isAuthenticated) return <Navigate to="/" replace />

  return children
}
