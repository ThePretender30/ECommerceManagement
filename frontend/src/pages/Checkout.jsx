import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Loader } from '../components/Common'
import { addressService } from '../services/catalogService'
import couponService from '../services/couponService'
import orderService from '../services/orderService'
import { useCart, useToast } from '../hooks'
import { formatCurrency, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './Checkout.css'

const EMPTY_ADDRESS = {
  fullName: '',
  phone: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'India',
}

export default function Checkout() {
  const { items, subtotal, checkoutAllowed, loading: cartLoading, reset } = useCart()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()

  const [addresses, setAddresses] = useState([])
  const [loadingAddresses, setLoadingAddresses] = useState(true)
  const [selectedAddressId, setSelectedAddressId] = useState(null)
  const [useNewAddress, setUseNewAddress] = useState(false)
  const [newAddress, setNewAddress] = useState(EMPTY_ADDRESS)
  const [saveNewAddress, setSaveNewAddress] = useState(true)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [placing, setPlacing] = useState(false)

  const [couponCodeInput, setCouponCodeInput] = useState('')
  const [applyingCoupon, setApplyingCoupon] = useState(false)
  const [appliedCoupon, setAppliedCoupon] = useState(null)

  useEffect(() => {
    addressService
      .list()
      .then((data) => {
        setAddresses(data)
        const preferred = data.find((a) => a.isDefault) ?? data[0]
        if (preferred) {
          setSelectedAddressId(preferred.id)
        } else {
          setUseNewAddress(true)
        }
      })
      .catch(() => setUseNewAddress(true))
      .finally(() => setLoadingAddresses(false))
  }, [])

  // Auto-apply coupon passed from Cart page
  useEffect(() => {
    const passedCode = location.state?.couponCode
    if (passedCode && subtotal > 0 && !appliedCoupon) {
      couponService
        .validate(passedCode, subtotal)
        .then((res) => {
          if (res.valid) setAppliedCoupon(res)
        })
        .catch(() => {})
    }
  }, [location.state, subtotal, appliedCoupon])

  useEffect(() => {
    if (!cartLoading && items.length === 0) {
      navigate('/cart', { replace: true })
    }
  }, [cartLoading, items.length, navigate])

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
  const finalCheckoutTotal = Math.max(0, Number(subtotal) - Number(discountAmount))

  const handleAddressChange = (event) => {
    const { name, value } = event.target
    setNewAddress((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [`newAddress.${name}`]: undefined }))
  }

  const handlePlaceOrder = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})

    if (!useNewAddress && !selectedAddressId) {
      setError('Please choose a delivery address.')
      return
    }

    setPlacing(true)
    try {
      const payload = useNewAddress
        ? { newAddress, saveNewAddress, couponCode: appliedCoupon?.code }
        : { addressId: selectedAddressId, couponCode: appliedCoupon?.code }

      const order = await orderService.place(payload)

      reset()
      toast.success(`Order ${order.orderNumber} placed successfully!`)
      navigate(`/orders/${order.id}/confirmation`, { replace: true })
    } catch (err) {
      setError(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } finally {
      setPlacing(false)
    }
  }

  if (cartLoading || loadingAddresses) return <Loader fullPage label="Preparing checkout…" />

  return (
    <div className="page container">
      <div className="page-header">
        <h1 className="page-title">Checkout</h1>
        <p className="page-subtitle">Review your order and confirm the delivery address.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!checkoutAllowed && (
        <div className="alert alert-warning">
          Some items in your cart are no longer fully in stock.{' '}
          <Link to="/cart">Return to your cart</Link> to adjust them.
        </div>
      )}

      <form className="checkout" onSubmit={handlePlaceOrder}>
        <div className="checkout-main">
          <section className="card">
            <div className="card-header">Delivery address</div>
            <div className="card-body">
              {addresses.length > 0 && (
                <div className="address-options">
                  {addresses.map((address) => (
                    <label
                      key={address.id}
                      className={
                        !useNewAddress && selectedAddressId === address.id
                          ? 'address-option is-selected'
                          : 'address-option'
                      }
                    >
                      <input
                        type="radio"
                        name="address"
                        checked={!useNewAddress && selectedAddressId === address.id}
                        onChange={() => {
                          setSelectedAddressId(address.id)
                          setUseNewAddress(false)
                        }}
                      />
                      <span className="address-option-body">
                        <span className="address-option-name">
                          {address.fullName}
                          {address.isDefault && <span className="badge badge-primary">Default</span>}
                        </span>
                        <span className="address-option-text">{address.formatted}</span>
                        <span className="address-option-phone">{address.phone}</span>
                      </span>
                    </label>
                  ))}

                  <label
                    className={useNewAddress ? 'address-option is-selected' : 'address-option'}
                  >
                    <input
                      type="radio"
                      name="address"
                      checked={useNewAddress}
                      onChange={() => setUseNewAddress(true)}
                    />
                    <span className="address-option-body">
                      <span className="address-option-name">Use a new address</span>
                      <span className="address-option-text">
                        Enter a different delivery address for this order.
                      </span>
                    </span>
                  </label>
                </div>
              )}

              {useNewAddress && (
                <div className={addresses.length > 0 ? 'new-address mt-6' : 'new-address'}>
                  <div className="form-row cols-2">
                    <div className="form-group">
                      <label className="form-label" htmlFor="fullName">Recipient name</label>
                      <input
                        id="fullName" name="fullName" type="text" required
                        className={fieldErrors['newAddress.fullName'] ? 'form-control has-error' : 'form-control'}
                        value={newAddress.fullName} onChange={handleAddressChange}
                        placeholder="Aisha Sharma"
                      />
                      {fieldErrors['newAddress.fullName'] && (
                        <span className="form-error">{fieldErrors['newAddress.fullName']}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="phone">Contact phone</label>
                      <input
                        id="phone" name="phone" type="tel" required
                        className={fieldErrors['newAddress.phone'] ? 'form-control has-error' : 'form-control'}
                        value={newAddress.phone} onChange={handleAddressChange}
                        placeholder="+919876543210"
                      />
                      {fieldErrors['newAddress.phone'] ? (
                        <span className="form-error">{fieldErrors['newAddress.phone']}</span>
                      ) : (
                        <span className="form-hint">Include the country code.</span>
                      )}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="line1">Address line 1</label>
                    <input
                      id="line1" name="line1" type="text" required
                      className={fieldErrors['newAddress.line1'] ? 'form-control has-error' : 'form-control'}
                      value={newAddress.line1} onChange={handleAddressChange}
                      placeholder="Flat / house number, building, street"
                    />
                    {fieldErrors['newAddress.line1'] && (
                      <span className="form-error">{fieldErrors['newAddress.line1']}</span>
                    )}
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="line2">Address line 2 (optional)</label>
                    <input
                      id="line2" name="line2" type="text" className="form-control"
                      value={newAddress.line2} onChange={handleAddressChange}
                      placeholder="Area, landmark"
                    />
                  </div>

                  <div className="form-row cols-3">
                    <div className="form-group">
                      <label className="form-label" htmlFor="city">City</label>
                      <input
                        id="city" name="city" type="text" required
                        className={fieldErrors['newAddress.city'] ? 'form-control has-error' : 'form-control'}
                        value={newAddress.city} onChange={handleAddressChange}
                      />
                      {fieldErrors['newAddress.city'] && (
                        <span className="form-error">{fieldErrors['newAddress.city']}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="state">State</label>
                      <input
                        id="state" name="state" type="text" required
                        className={fieldErrors['newAddress.state'] ? 'form-control has-error' : 'form-control'}
                        value={newAddress.state} onChange={handleAddressChange}
                      />
                      {fieldErrors['newAddress.state'] && (
                        <span className="form-error">{fieldErrors['newAddress.state']}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="postalCode">Postal code</label>
                      <input
                        id="postalCode" name="postalCode" type="text" required
                        className={fieldErrors['newAddress.postalCode'] ? 'form-control has-error' : 'form-control'}
                        value={newAddress.postalCode} onChange={handleAddressChange}
                      />
                      {fieldErrors['newAddress.postalCode'] && (
                        <span className="form-error">{fieldErrors['newAddress.postalCode']}</span>
                      )}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="country">Country</label>
                    <input
                      id="country" name="country" type="text" className="form-control"
                      value={newAddress.country} onChange={handleAddressChange}
                    />
                  </div>

                  <label className="filter-checkbox">
                    <input
                      type="checkbox"
                      checked={saveNewAddress}
                      onChange={(event) => setSaveNewAddress(event.target.checked)}
                    />
                    <span>Save this address to my address book</span>
                  </label>
                </div>
              )}
            </div>
          </section>

          <section className="card mt-6">
            <div className="card-header">
              Review your order
              <Link to="/cart" className="text-sm">Edit cart</Link>
            </div>
            <div className="card-body">
              <div className="review-items">
                {items.map((item) => (
                  <div key={item.id} className="review-item-row">
                    <img
                      src={item.productImageUrl || FALLBACK_IMAGE}
                      alt={item.productName}
                      onError={handleImageError}
                    />
                    <div className="review-item-info">
                      <span className="review-item-name">{item.productName}</span>
                      <span className="text-xs text-muted">
                        {formatCurrency(item.unitPrice)} × {item.quantity}
                      </span>
                    </div>
                    <span className="review-item-total">{formatCurrency(item.lineTotal)}</span>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>

        <aside className="checkout-summary">
          <h2 className="cart-summary-title">Order total</h2>

          <div className="coupon-box">
            <span className="coupon-label">Promo Code / Coupon</span>
            {!appliedCoupon ? (
              <div className="coupon-form">
                <input
                  type="text"
                  className="form-control coupon-input"
                  placeholder="e.g. WELCOME10"
                  value={couponCodeInput}
                  onChange={(e) => setCouponCodeInput(e.target.value.toUpperCase())}
                  disabled={applyingCoupon}
                />
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleApplyCoupon}
                  disabled={applyingCoupon || !couponCodeInput.trim()}
                >
                  {applyingCoupon ? 'Applying…' : 'Apply'}
                </button>
              </div>
            ) : (
              <div className="coupon-applied-tag">
                <div className="coupon-applied-info">
                  <span className="coupon-code-badge">🏷️ {appliedCoupon.code}</span>
                  <span className="text-xs text-success">
                    {appliedCoupon.discountType === 'PERCENTAGE'
                      ? `${appliedCoupon.discountValue}% off`
                      : `${formatCurrency(appliedCoupon.discountValue)} flat off`}
                  </span>
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
            )}
          </div>

          <div className="cart-summary-row">
            <span>Items ({items.length})</span>
            <span>{formatCurrency(subtotal)}</span>
          </div>

          {appliedCoupon && (
            <div className="cart-summary-row text-success">
              <span>Coupon Discount ({appliedCoupon.code})</span>
              <span>-{formatCurrency(discountAmount)}</span>
            </div>
          )}

          <div className="cart-summary-row">
            <span>Delivery</span>
            <span className="text-success">Free</span>
          </div>
          <div className="cart-summary-row is-total">
            <span>Total</span>
            <span>{formatCurrency(finalCheckoutTotal)}</span>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block btn-lg mt-4"
            disabled={placing || !checkoutAllowed}
          >
            {placing ? 'Placing your order…' : 'Place order'}
          </button>

          <p className="cart-summary-note">
            You will receive a WhatsApp confirmation once your order is placed.
          </p>
        </aside>
      </form>
    </div>
  )
}
