import { useContext, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'
import { CartContext } from '../context/CartContext'
import { ToastContext } from '../context/ToastContext'

/**
 * Context accessors.
 *
 * Each throws when used outside its provider - a clear error at the point of
 * misuse beats a confusing "cannot read property of null" further down.
 */

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

/**
 * Delays a rapidly-changing value.
 *
 * Used for search-as-you-type so a request is sent once the user pauses,
 * rather than on every keystroke.
 */
export function useDebounce(value, delay = 400) {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])

  return debounced
}

/**
 * Reads and writes filter state through the URL query string.
 *
 * Keeping filters, sort and page in the URL means results are shareable and
 * bookmarkable, and the browser's back button moves through filter changes the
 * way users expect - none of which works if that state lives only in React.
 */
export function useQueryParams() {
  const [searchParams, setSearchParams] = useSearchParams()

  const get = (key, fallback = '') => searchParams.get(key) ?? fallback

  /** Merges updates; a null/empty value removes the parameter entirely. */
  const setParams = (updates, { resetPage = true } = {}) => {
    const next = new URLSearchParams(searchParams)

    Object.entries(updates).forEach(([key, value]) => {
      if (value === null || value === undefined || value === '') {
        next.delete(key)
      } else {
        next.set(key, String(value))
      }
    })

    // Changing a filter should send the user back to page 1; otherwise they can
    // land on an empty page 5 of a much smaller result set.
    if (resetPage && !('page' in updates)) {
      next.delete('page')
    }

    setSearchParams(next)
  }

  const clearAll = () => setSearchParams(new URLSearchParams())

  return { searchParams, get, setParams, clearAll }
}

/** Scrolls to the top whenever the value changes (e.g. on page navigation). */
export function useScrollToTop(dependency) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [dependency])
}
