import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { cartService } from '../services/catalogService'
import { AuthContext } from './AuthContext'

export const CartContext = createContext(null)

/**
 * Mirrors the server-side cart.
 *
 * The cart lives in the database, not in this state - every mutation calls the
 * API and replaces local state with whatever the server returns. That means
 * quantities, totals and stock warnings always come from the authoritative
 * source, and the cart survives switching devices.
 */
export function CartProvider({ children }) {
  const { isAuthenticated } = useContext(AuthContext)
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(false)

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setCart(null)
      return null
    }
    setLoading(true)
    try {
      const data = await cartService.get()
      setCart(data)
      return data
    } catch {
      // A cart failure should never break the page the user is on.
      setCart(null)
      return null
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

  // Load on sign-in, drop on sign-out.
  useEffect(() => {
    refresh()
  }, [refresh])

  const addItem = useCallback(async (productId, quantity = 1) => {
    const updated = await cartService.addItem(productId, quantity)
    setCart(updated)
    return updated
  }, [])

  const updateItem = useCallback(async (itemId, quantity) => {
    const updated = await cartService.updateItem(itemId, quantity)
    setCart(updated)
    return updated
  }, [])

  const removeItem = useCallback(async (itemId) => {
    const updated = await cartService.removeItem(itemId)
    setCart(updated)
    return updated
  }, [])

  const clear = useCallback(async () => {
    const updated = await cartService.clear()
    setCart(updated)
    return updated
  }, [])

  /** Called after checkout, when the backend has already emptied the cart. */
  const reset = useCallback(() => setCart(null), [])

  const value = useMemo(
    () => ({
      cart,
      loading,
      items: cart?.items ?? [],
      itemCount: cart?.totalQuantity ?? 0,
      subtotal: cart?.subtotal ?? 0,
      total: cart?.total ?? 0,
      checkoutAllowed: cart?.checkoutAllowed ?? false,
      refresh,
      addItem,
      updateItem,
      removeItem,
      clear,
      reset,
    }),
    [cart, loading, refresh, addItem, updateItem, removeItem, clear, reset]
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
