import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Breadcrumbs, ErrorState, Loader } from '../components/Common'
import { CartIcon } from '../components/Icons'
import { ConfirmDialog } from '../components/Modal'
import OrderStatusTimeline from '../components/OrderStatusTimeline'
import orderService from '../services/orderService'
import { useCart, useToast } from '../hooks'
import {
  formatCurrency,
  formatDateTime,
  handleImageError,
  statusBadgeClass,
  FALLBACK_IMAGE,
} from '../utils/format'
import './Orders.css'

export default function OrderTracking() {
  const { id } = useParams()
  const { addItem } = useCart()
  const toast = useToast()
  const navigate = useNavigate()

  const [order, setOrder] = useState(null)
  const [tracking, setTracking] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [reordering, setReordering] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)

    Promise.all([orderService.getById(id), orderService.track(id)])
      .then(([orderData, trackingData]) => {
        setOrder(orderData)
        setTracking(trackingData)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(load, [load])

  const handleCancel = async () => {
    setCancelling(true)
    try {
      await orderService.cancel(id)
      toast.success('Your order has been cancelled and the items returned to stock.')
      setConfirmCancel(false)
      load()
    } catch (err) {
      toast.error(err.message)
      setConfirmCancel(false)
    } finally {
      setCancelling(false)
    }
  }

  const handleBuyAgain = async () => {
    if (!order?.items || order.items.length === 0) {
      toast.info('No items found to reorder.')
      return
    }

    setReordering(true)
    let addedCount = 0
    let failedCount = 0

    try {
      for (const item of order.items) {
        if (item.productId) {
          try {
            await addItem(item.productId, item.quantity || 1)
            addedCount++
          } catch {
            failedCount++
          }
        }
      }

      if (addedCount > 0) {
        toast.success(
          `Added ${addedCount} ${addedCount === 1 ? 'item' : 'items'} from Order #${order.orderNumber} to your cart!`
        )
        navigate('/cart')
      } else if (failedCount > 0) {
        toast.error('Items from this order are currently out of stock.')
      }
    } catch (err) {
      toast.error(err.message || 'Failed to reorder items.')
    } finally {
      setReordering(false)
    }
  }

  if (loading) return <Loader fullPage label="Loading your order…" />
  if (error) return <ErrorState message={error} onRetry={load} />
  if (!order) return null

  return (
    <div className="page container">
      <Breadcrumbs
        items={[
          { label: 'Home', to: '/' },
          { label: 'My Orders', to: '/orders' },
          { label: order.orderNumber },
        ]}
      />

      <div className="page-header order-header">
        <div>
          <h1 className="page-title">{order.orderNumber}</h1>
          <p className="page-subtitle">Placed on {formatDateTime(order.placedAt)}</p>
        </div>
        <div className="order-header-actions">
          <span className={`badge ${statusBadgeClass(order.status)}`}>{order.statusLabel}</span>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={handleBuyAgain}
            disabled={reordering}
            title="Add all items from this order back to your cart"
          >
            <CartIcon size={14} />
            {reordering ? 'Adding…' : 'Buy again'}
          </button>
          {order.cancellable && (
            <button
              type="button"
              className="btn btn-danger btn-sm"
              onClick={() => setConfirmCancel(true)}
            >
              Cancel order
            </button>
          )}
        </div>
      </div>

      <section className="card mb-6">
        <div className="card-header">Order tracking</div>
        <div className="card-body">
          <OrderStatusTimeline steps={tracking?.progressSteps} cancelled={tracking?.cancelled} />
        </div>
      </section>

      <div className="order-detail-grid">
        <section className="card">
          <div className="card-header">Items ({order.items.length})</div>
          <div className="card-body">
            <div className="review-items">
              {order.items.map((item) => (
                <div key={item.id} className="review-item-row">
                  <img
                    src={item.productImageUrl || FALLBACK_IMAGE}
                    alt={item.productName}
                    onError={handleImageError}
                  />
                  <div className="review-item-info">
                    {item.productAvailable && item.productId ? (
                      <Link to={`/products/${item.productId}`} className="review-item-name">
                        {item.productName}
                      </Link>
                    ) : (
                      <span className="review-item-name">{item.productName}</span>
                    )}
                    <span className="text-xs text-muted">
                      {formatCurrency(item.unitPrice)} × {item.quantity}
                    </span>
                  </div>
                  <span className="review-item-total">{formatCurrency(item.lineTotal)}</span>
                </div>
              ))}
            </div>

            <div className="cart-summary-row mt-4">
              <span>Subtotal</span>
              <span>{formatCurrency(order.subtotalAmount || order.totalAmount)}</span>
            </div>
            {order.couponCode && order.discountAmount > 0 && (
              <div className="cart-summary-row text-success">
                <span>Coupon ({order.couponCode})</span>
                <span>-{formatCurrency(order.discountAmount)}</span>
              </div>
            )}
            <div className="cart-summary-row is-total">
              <span>Order total</span>
              <span>{formatCurrency(order.totalAmount)}</span>
            </div>

            {order.status === 'DELIVERED' && (
              <p className="text-xs text-muted mt-4">
                Delivered — you can now review any of these products from their product pages.
              </p>
            )}
          </div>
        </section>

        <div className="stack">
          <section className="card">
            <div className="card-header">Delivery address</div>
            <div className="card-body">
              <dl className="detail-list">
                <div>
                  <dt>Recipient</dt>
                  <dd>{order.deliveryFullName}</dd>
                </div>
                <div>
                  <dt>Contact</dt>
                  <dd>{order.deliveryPhone}</dd>
                </div>
                <div>
                  <dt>Address</dt>
                  <dd>{order.deliveryAddress}</dd>
                </div>
              </dl>
            </div>
          </section>

          <section className="card">
            <div className="card-header">Status history</div>
            <div className="card-body">
              <ul className="history-list">
                {order.statusHistory.map((entry) => (
                  <li key={entry.id} className="history-item">
                    <div className="row-between">
                      <span className="font-semibold text-sm">{entry.statusLabel}</span>
                      <span className="text-xs text-subtle">{formatDateTime(entry.changedAt)}</span>
                    </div>
                    {entry.note && <p className="text-xs text-muted mt-2">{entry.note}</p>}
                  </li>
                ))}
              </ul>
            </div>
          </section>
        </div>
      </div>

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel this order?"
        message="The items will be returned to stock and you will receive a WhatsApp confirmation. This cannot be undone."
        confirmLabel="Yes, cancel it"
        cancelLabel="Keep my order"
        danger
        busy={cancelling}
        onConfirm={handleCancel}
        onCancel={() => setConfirmCancel(false)}
      />
    </div>
  )
}
