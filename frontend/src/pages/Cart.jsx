import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { EmptyState, Loader, QuantityStepper } from '../components/Common'
import { ConfirmDialog } from '../components/Modal'
import { useCart, useToast } from '../hooks'
import { formatCurrency, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './Cart.css'

/**
 * The shopping cart.
 *
 * Every total shown here comes from the server, and each line carries its own
 * stock status - so an item that sold out after being added is flagged here,
 * before the customer reaches checkout and hits a failure.
 */
export default function Cart() {
  const { cart, items, loading, subtotal, total, checkoutAllowed, updateItem, removeItem, clear } =
    useCart()
  const toast = useToast()
  const navigate = useNavigate()

  const [busyItemId, setBusyItemId] = useState(null)
  const [confirmClear, setConfirmClear] = useState(false)

  const handleQuantityChange = async (item, quantity) => {
    setBusyItemId(item.id)
    try {
      await updateItem(item.id, quantity)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusyItemId(null)
    }
  }

  const handleRemove = async (item) => {
    setBusyItemId(item.id)
    try {
      await removeItem(item.id)
      toast.info(`${item.productName} removed from your cart.`)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusyItemId(null)
    }
  }

  const handleClear = async () => {
    try {
      await clear()
      toast.info('Your cart has been emptied.')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setConfirmClear(false)
    }
  }

  if (loading && !cart) return <Loader fullPage label="Loading your cart…" />

  if (!items.length) {
    return (
      <div className="page container">
        <EmptyState
          icon="🛒"
          title="Your cart is empty"
          message="Browse the store and add something you like — your cart is saved to your account."
          action={<Link to="/products" className="btn btn-primary btn-lg">Start shopping</Link>}
        />
      </div>
    )
  }

  const outOfStockLines = items.filter((item) => !item.stockSufficient)

  return (
    <div className="page container">
      <div className="page-header">
        <h1 className="page-title">Shopping cart</h1>
        <p className="page-subtitle">
          {items.length} {items.length === 1 ? 'item' : 'items'} in your cart
        </p>
      </div>

      {outOfStockLines.length > 0 && (
        <div className="alert alert-warning">
          Some items no longer have enough stock. Reduce the quantity or remove them to continue.
        </div>
      )}

      <div className="cart">
        <div className="cart-items">
          {items.map((item) => (
            <article
              key={item.id}
              className={item.stockSufficient ? 'cart-item' : 'cart-item has-issue'}
            >
              <Link to={`/products/${item.productId}`} className="cart-item-image">
                <img
                  src={item.productImageUrl || FALLBACK_IMAGE}
                  alt={item.productName}
                  onError={handleImageError}
                />
              </Link>

              <div className="cart-item-info">
                <Link to={`/products/${item.productId}`} className="cart-item-name">
                  {item.productName}
                </Link>
                {item.categoryName && (
                  <span className="cart-item-category">{item.categoryName}</span>
                )}
                <span className="cart-item-unit">{formatCurrency(item.unitPrice)} each</span>

                {!item.stockSufficient && (
                  <span className="cart-item-warning">
                    Only {item.availableStock} left — reduce the quantity to continue.
                  </span>
                )}
              </div>

              <div className="cart-item-qty">
                <QuantityStepper
                  value={item.quantity}
                  onChange={(quantity) => handleQuantityChange(item, quantity)}
                  min={1}
                  max={Math.max(item.availableStock, 1)}
                  disabled={busyItemId === item.id}
                />
              </div>

              <div className="cart-item-total">
                <span className="cart-item-price">{formatCurrency(item.lineTotal)}</span>
                <button
                  type="button"
                  className="cart-item-remove"
                  onClick={() => handleRemove(item)}
                  disabled={busyItemId === item.id}
                >
                  Remove
                </button>
              </div>
            </article>
          ))}

          <div className="cart-actions">
            <Link to="/products" className="btn btn-ghost btn-sm">← Continue shopping</Link>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => setConfirmClear(true)}
            >
              Empty cart
            </button>
          </div>
        </div>

        {/* Summary sticks to the viewport on desktop so the checkout button is
            always reachable, however long the item list gets. */}
        <aside className="cart-summary">
          <h2 className="cart-summary-title">Order summary</h2>

          <div className="cart-summary-row">
            <span>Subtotal</span>
            <span>{formatCurrency(subtotal)}</span>
          </div>
          <div className="cart-summary-row">
            <span>Delivery</span>
            <span className="text-success">Free</span>
          </div>

          <div className="cart-summary-row is-total">
            <span>Total</span>
            <span>{formatCurrency(total)}</span>
          </div>

          <button
            type="button"
            className="btn btn-primary btn-block btn-lg mt-4"
            onClick={() => navigate('/checkout')}
            disabled={!checkoutAllowed}
          >
            Proceed to checkout
          </button>

          {!checkoutAllowed && (
            <p className="text-xs text-muted mt-2 text-center">
              Resolve the stock issues above to check out.
            </p>
          )}

          <p className="cart-summary-note">
            You will review your order and delivery address before it is placed.
          </p>
        </aside>
      </div>

      <ConfirmDialog
        open={confirmClear}
        title="Empty your cart?"
        message="This removes every item from your cart. It cannot be undone."
        confirmLabel="Empty cart"
        danger
        onConfirm={handleClear}
        onCancel={() => setConfirmClear(false)}
      />
    </div>
  )
}
