import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import authService from '../services/authService'
import { setUnauthorizedHandler, tokenStorage, userStorage } from '../services/api'

export const AuthContext = createContext(null)

/**
 * Holds who is signed in.
 *
 * The user is cached in localStorage so a page refresh does not flash the app
 * back to a signed-out state while /auth/me is in flight. The cache is then
 * revalidated against the server, because a stored user object says nothing
 * about whether the token is still good.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => userStorage.get())
  const [loading, setLoading] = useState(true)

  const signOut = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
  }, [])

  // The axios interceptor calls this when the server rejects our token.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      tokenStorage.clear()
      setUser(null)
    })
  }, [])

  // Revalidate the cached session on first load.
  useEffect(() => {
    const token = tokenStorage.get()
    if (!token) {
      setUser(null)
      setLoading(false)
      return
    }

    let cancelled = false
    authService
      .me()
      .then((fresh) => {
        if (cancelled) return
        setUser(fresh)
        userStorage.set(fresh)
      })
      .catch(() => {
        // Expired or invalid token; the interceptor has already cleared storage.
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const applySession = useCallback((authResponse) => {
    tokenStorage.set(authResponse.token)
    userStorage.set(authResponse.user)
    setUser(authResponse.user)
    return authResponse.user
  }, [])

  const login = useCallback(
    async (email, password) => applySession(await authService.login(email, password)),
    [applySession]
  )

  const register = useCallback(
    async (payload) => applySession(await authService.register(payload)),
    [applySession]
  )

  const logout = useCallback(async () => {
    try {
      await authService.logout()
    } catch {
      // Stateless JWT: the server has nothing to invalidate, so a failed call
      // here must not stop the client from forgetting its token.
    }
    signOut()
  }, [signOut])

  /** Applies a profile update without forcing a full reload. */
  const updateUser = useCallback((updated) => {
    setUser(updated)
    userStorage.set(updated)
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: Boolean(user),
      isAdmin: Boolean(user?.roles?.includes('ROLE_ADMIN')),
      login,
      register,
      logout,
      updateUser,
    }),
    [user, loading, login, register, logout, updateUser]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
