import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Breadcrumbs, ErrorState, Loader } from '../components/Common'
import { ConfirmDialog } from '../components/Modal'
import OrderStatusTimeline from '../components/OrderStatusTimeline'
import orderService from '../services/orderService'
import { useToast } from '../hooks'
import {
  formatCurrency,
  formatDateTime,
  handleImageError,
  statusBadgeClass,
  FALLBACK_IMAGE,
} from '../utils/format'
import './Orders.css'

/**
 * Order detail and tracking.
 *
 * Two calls back this page: the order itself, and its tracking timeline. The
 * timeline's step states come from the backend, so a cancelled order shows an
 * honest short timeline rather than a progress bar frozen halfway.
 */
export default function OrderTracking() {
  const { id } = useParams()
  const toast = useToast()

  const [order, setOrder] = useState(null)
  const [tracking, setTracking] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [cancelling, setCancelling] = useState(false)

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
          {order.cancellable && (
            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => setConfirmCancel(true)}
            >
              Cancel order
            </button>
          )}
        </div>
      </div>

      {/* ---------------- Tracking ---------------- */}
      <section className="card mb-6">
        <div className="card-header">Order tracking</div>
        <div className="card-body">
          <OrderStatusTimeline steps={tracking?.progressSteps} cancelled={tracking?.cancelled} />
        </div>
      </section>

      <div className="order-detail-grid">
        {/* ---------------- Items ---------------- */}
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

        {/* ---------------- Delivery + history ---------------- */}
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
