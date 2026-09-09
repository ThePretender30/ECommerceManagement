import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import authService from '../services/authService'
import { setUnauthorizedHandler, tokenStorage, userStorage } from '../services/api'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => userStorage.get())
  const [loading, setLoading] = useState(true)

  const signOut = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      tokenStorage.clear()
      setUser(null)
    })
  }, [])

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

  const initiateLogin = useCallback(
    async (email, password) => authService.login(email, password),
    []
  )

  const initiateAdminLogin = useCallback(
    async (email, password) => authService.loginAdmin(email, password),
    []
  )

  const verifyOtp = useCallback(
    async (sessionToken, otp) => applySession(await authService.verifyOtp(sessionToken, otp)),
    [applySession]
  )

  const resendOtp = useCallback(
    async (sessionToken) => authService.resendOtp(sessionToken),
    []
  )

  const register = useCallback(
    async (payload) => applySession(await authService.register(payload)),
    [applySession]
  )

  const logout = useCallback(async () => {
    try {
      await authService.logout()
    } catch {
    }
    signOut()
  }, [signOut])

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
      initiateLogin,
      initiateAdminLogin,
      verifyOtp,
      resendOtp,
      register,
      logout,
      updateUser,
    }),
    [user, loading, initiateLogin, initiateAdminLogin, verifyOtp, resendOtp, register, logout, updateUser]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
