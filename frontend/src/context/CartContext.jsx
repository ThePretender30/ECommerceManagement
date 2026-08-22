import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { cartService } from '../services/catalogService'
import { AuthContext } from './AuthContext'

export const CartContext = createContext(null)

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
      setCart(null)
      return null
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

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
