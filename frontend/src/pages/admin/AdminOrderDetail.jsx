import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ErrorState, Loader } from '../../components/Common'
import OrderStatusTimeline from '../../components/OrderStatusTimeline'
import adminService from '../../services/adminService'
import { useToast } from '../../hooks'
import {
  formatCurrency,
  formatDateTime,
  handleImageError,
  statusBadgeClass,
  FALLBACK_IMAGE,
} from '../../utils/format'

export default function AdminOrderDetail() {
  const { id } = useParams()
  const toast = useToast()

  const [order, setOrder] = useState(null)
  const [statuses, setStatuses] = useState([])
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [nextStatus, setNextStatus] = useState('')
  const [note, setNote] = useState('')
  const [updating, setUpdating] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)

    Promise.all([
      adminService.getOrder(id),
      adminService.orderStatuses(),
      adminService.orderNotifications(id).catch(() => []),
    ])
      .then(([orderData, statusData, notificationData]) => {
        setOrder(orderData)
        setStatuses(statusData)
        setNotifications(notificationData)
        setNextStatus('')
        setNote('')
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(load, [load])

  const handleStatusUpdate = async (event) => {
    event.preventDefault()
    if (!nextStatus) return

    setUpdating(true)
    try {
      await adminService.updateOrderStatus(id, nextStatus, note || null)
      toast.success('Order status updated. The customer has been notified on WhatsApp.')
      load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setUpdating(false)
    }
  }

  if (loading) return <Loader fullPage label="Loading order…" />
  if (error) return <ErrorState message={error} onRetry={load} />
  if (!order) return null

  const currentStatusInfo = statuses.find((s) => s.value === order.status)
  const allowedNext = currentStatusInfo?.allowedNext ?? []
  const statusLabelOf = (value) => statuses.find((s) => s.value === value)?.label ?? value

  const happyPath = [
    'ORDER_PLACED', 'ORDER_CONFIRMED', 'PROCESSING',
    'DISPATCHED', 'OUT_FOR_DELIVERY', 'DELIVERED',
  ]
  const reachedAt = Object.fromEntries(
    order.statusHistory.map((entry) => [entry.status, entry.changedAt])
  )
  const cancelled = order.status === 'CANCELLED'
  const currentIndex = happyPath.indexOf(order.status)
  const steps = cancelled
    ? [
        { status: 'ORDER_PLACED', label: 'Order Placed', state: 'completed', occurredAt: reachedAt.ORDER_PLACED },
        { status: 'CANCELLED', label: 'Cancelled', state: 'current', occurredAt: reachedAt.CANCELLED },
      ]
    : happyPath.map((stage, index) => ({
        status: stage,
        label: statusLabelOf(stage),
        state: index < currentIndex ? 'completed' : index === currentIndex ? 'current' : 'pending',
        occurredAt: reachedAt[stage],
      }))

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">{order.orderNumber}</h1>
          <p className="admin-subtitle">Placed {formatDateTime(order.placedAt)}</p>
        </div>
        <div className="row wrap">
          <span className={`badge ${statusBadgeClass(order.status)}`}>{order.statusLabel}</span>
          <Link to="/admin/orders" className="btn btn-outline btn-sm">← All orders</Link>
        </div>
      </div>

      <section className="card mb-6">
        <div className="card-header">Progress</div>
        <div className="card-body">
          <OrderStatusTimeline steps={steps} cancelled={cancelled} />
        </div>
      </section>

      <div className="admin-grid split">
        <div className="stack">
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
                      <span className="review-item-name">{item.productName}</span>
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
            </div>
          </section>

          <section className="card">
            <div className="card-header">WhatsApp notifications</div>
            <div className="card-body">
              {notifications.length === 0 ? (
                <p className="text-sm text-muted">No notifications recorded for this order yet.</p>
              ) : (
                <ul className="stack">
                  {notifications.map((notification) => (
                    <li key={notification.id} className="history-item">
                      <div className="row-between wrap">
                        <span className="text-sm font-semibold">
                          {notification.triggerStatus?.replaceAll('_', ' ') ?? 'Notification'}
                        </span>
                        <span
                          className={
                            notification.status === 'SENT' ? 'badge badge-success'
                            : notification.status === 'FAILED' ? 'badge badge-danger'
                            : 'badge badge-neutral'
                          }
                        >
                          {notification.status}
                        </span>
                      </div>
                      <p className="text-xs text-muted mt-2" style={{ whiteSpace: 'pre-line' }}>
                        {notification.message}
                      </p>
                      <div className="text-xs text-subtle mt-2">
                        To {notification.recipient} · {formatDateTime(notification.createdAt)}
                      </div>
                      {notification.errorMessage && (
                        <p className="text-xs mt-2" style={{ color: 'var(--color-danger)' }}>
                          {notification.errorMessage}
                        </p>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
        </div>

        <div className="stack">
          <section className="card">
            <div className="card-header">Update status</div>
            <div className="card-body">
              {allowedNext.length === 0 ? (
                <p className="text-sm text-muted">
                  This order is in a final state ({order.statusLabel}) and cannot change further.
                </p>
              ) : (
                <form onSubmit={handleStatusUpdate}>
                  <div className="form-group">
                    <label className="form-label" htmlFor="nextStatus">New status</label>
                    <select
                      id="nextStatus"
                      className="form-control"
                      value={nextStatus}
                      onChange={(event) => setNextStatus(event.target.value)}
                      required
                    >
                      <option value="">Choose the next step…</option>
                      {allowedNext.map((value) => (
                        <option key={value} value={value}>{statusLabelOf(value)}</option>
                      ))}
                    </select>
                    <span className="form-hint">
                      Only transitions valid from “{order.statusLabel}” are listed.
                    </span>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="note">Note (optional)</label>
                    <textarea
                      id="note"
                      className="form-control"
                      rows="2"
                      maxLength={500}
                      value={note}
                      onChange={(event) => setNote(event.target.value)}
                      placeholder="Recorded in the order's status history."
                    />
                  </div>

                  <button
                    type="submit"
                    className="btn btn-primary btn-block"
                    disabled={updating || !nextStatus}
                  >
                    {updating ? 'Updating…' : 'Update and notify customer'}
                  </button>

                  {nextStatus === 'CANCELLED' && (
                    <p className="alert alert-warning mt-4 text-xs">
                      Cancelling returns every item in this order to stock.
                    </p>
                  )}
                </form>
              )}
            </div>
          </section>

          <section className="card">
            <div className="card-header">Customer</div>
            <div className="card-body">
              <dl className="detail-list">
                <div>
                  <dt>Name</dt>
                  <dd>{order.customerName}</dd>
                </div>
                <div>
                  <dt>Email</dt>
                  <dd>{order.customerEmail}</dd>
                </div>
                <div>
                  <dt>Account phone</dt>
                  <dd>{order.customerPhone}</dd>
                </div>
              </dl>
            </div>
          </section>

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
        </div>
      </div>
    </div>
  )
}
