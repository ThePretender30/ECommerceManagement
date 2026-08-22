import { useContext, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { CartContext } from '../context/CartContext'
import { ToastContext } from '../context/ToastContext'

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside an <AuthProvider>')
  return context
}

export function useCart() {
  const context = useContext(CartContext)
  if (!context) throw new Error('useCart must be used inside a <CartProvider>')
  return context
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast must be used inside a <ToastProvider>')
  return context
}

export function useDebounce(value, delay = 400) {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])

  return debounced
}

export function useQueryParams() {
  const [searchParams, setSearchParams] = useSearchParams()

  const get = (key, fallback = '') => searchParams.get(key) ?? fallback

  const setParams = (updates, { resetPage = true } = {}) => {
    const next = new URLSearchParams(searchParams)

    Object.entries(updates).forEach(([key, value]) => {
      if (value === null || value === undefined || value === '') {
        next.delete(key)
      } else {
        next.set(key, String(value))
      }
    })

    if (resetPage && !('page' in updates)) {
      next.delete('page')
    }

    setSearchParams(next)
  }

  const clearAll = () => setSearchParams(new URLSearchParams())

  return { searchParams, get, setParams, clearAll }
}

export function useScrollToTop(dependency) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [dependency])
}
