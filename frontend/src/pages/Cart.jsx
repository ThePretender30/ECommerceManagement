import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { EmptyState, Loader, QuantityStepper } from '../components/Common'
import { CartIcon } from '../components/Icons'
import { ConfirmDialog } from '../components/Modal'
import { useCart, useToast } from '../hooks'
import couponService from '../services/couponService'
import { formatCurrency, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './Cart.css'

export default function Cart() {
  const { cart, items, loading, subtotal, checkoutAllowed, updateItem, removeItem, clear } =
    useCart()
  const toast = useToast()
  const navigate = useNavigate()

  const [busyItemId, setBusyItemId] = useState(null)
  const [confirmClear, setConfirmClear] = useState(false)
  const [couponCodeInput, setCouponCodeInput] = useState('')
  const [applyingCoupon, setApplyingCoupon] = useState(false)
  const [appliedCoupon, setAppliedCoupon] = useState(null)

  const handleApplyCoupon = async (e) => {
    e.preventDefault()
    if (!couponCodeInput.trim()) return

    setApplyingCoupon(true)
    try {
      const res = await couponService.validate(couponCodeInput.trim(), subtotal)
      if (res.valid) {
        setAppliedCoupon(res)
        toast.success(`Coupon ${res.code} applied! You saved ${formatCurrency(res.discountAmount)}.`)
        setCouponCodeInput('')
      } else {
        toast.error(res.message || 'Invalid coupon code.')
      }
    } catch (err) {
      toast.error(err.message || 'Failed to apply coupon.')
    } finally {
      setApplyingCoupon(false)
    }
  }

  const handleRemoveCoupon = () => {
    setAppliedCoupon(null)
    toast.info('Coupon removed.')
  }

  const discountAmount = appliedCoupon ? appliedCoupon.discountAmount : 0
  const finalCartTotal = Math.max(0, Number(subtotal) - Number(discountAmount))

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
          icon={<CartIcon size={48} />}
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

        <aside className="cart-summary">
          <h2 className="cart-summary-title">Order summary</h2>

          <div className="coupon-box">
            <span className="coupon-label">Promotions & Coupons</span>
            {appliedCoupon ? (
              <div className="coupon-applied-tag">
                <div className="coupon-applied-info">
                  <span className="coupon-code-badge">✓ {appliedCoupon.code}</span>
                  <span>Saved {formatCurrency(appliedCoupon.discountAmount)}</span>
                </div>
                <button
                  type="button"
                  className="coupon-remove-btn"
                  onClick={handleRemoveCoupon}
                  title="Remove coupon"
                >
                  ✕
                </button>
              </div>
            ) : (
              <form onSubmit={handleApplyCoupon} className="coupon-form">
                <input
                  type="text"
                  className="form-control coupon-input"
                  placeholder="Enter coupon code"
                  value={couponCodeInput}
                  onChange={(e) => setCouponCodeInput(e.target.value.toUpperCase())}
                  disabled={applyingCoupon}
                />
                <button
                  type="submit"
                  className="btn btn-outline btn-sm"
                  disabled={applyingCoupon || !couponCodeInput.trim()}
                >
                  {applyingCoupon ? 'Applying…' : 'Apply'}
                </button>
              </form>
            )}
          </div>

          <div className="cart-summary-row">
            <span>Subtotal</span>
            <span>{formatCurrency(subtotal)}</span>
          </div>

          {appliedCoupon && (
            <div className="cart-summary-row text-success">
              <span>Coupon Discount ({appliedCoupon.code})</span>
              <span>-{formatCurrency(appliedCoupon.discountAmount)}</span>
            </div>
          )}

          <div className="cart-summary-row">
            <span>Delivery</span>
            <span className="text-success">Free</span>
          </div>

          <div className="cart-summary-row is-total">
            <span>Total</span>
            <span>{formatCurrency(finalCartTotal)}</span>
          </div>

          <button
            type="button"
            className="btn btn-primary btn-block btn-lg mt-4"
            onClick={() => navigate('/checkout', { state: { couponCode: appliedCoupon?.code } })}
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
