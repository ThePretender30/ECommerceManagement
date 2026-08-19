import { createContext, useCallback, useMemo, useRef, useState } from 'react'
import Toast from '../components/Toast'

export const ToastContext = createContext(null)

/**
 * App-wide transient notifications.
 *
 * Kept in context rather than per-page state so that an action which navigates
 * away (placing an order, deleting a product) can still report its result on
 * the page the user lands on.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const show = useCallback(
    (message, type = 'info', duration = 4000) => {
      const id = nextId.current++
      setToasts((current) => [...current, { id, message, type }])
      if (duration > 0) {
        setTimeout(() => dismiss(id), duration)
      }
      return id
    },
    [dismiss]
  )

  const value = useMemo(
    () => ({
      show,
      success: (message, duration) => show(message, 'success', duration),
      error: (message, duration) => show(message, 'error', duration ?? 6000),
      info: (message, duration) => show(message, 'info', duration),
      warning: (message, duration) => show(message, 'warning', duration),
      dismiss,
    }),
    [show, dismiss]
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toast toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  )
}
