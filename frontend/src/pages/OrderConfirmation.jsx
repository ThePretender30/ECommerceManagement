import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ErrorState, Loader } from '../components/Common'
import orderService from '../services/orderService'
import { formatCurrency, formatDateTime, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './Orders.css'

export default function OrderConfirmation() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    orderService
      .getById(id)
      .then(setOrder)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <Loader fullPage label="Loading your order…" />
  if (error) return <ErrorState message={error} />
  if (!order) return null

  return (
    <div className="page container">
      <div className="confirmation">
        <div className="confirmation-icon" aria-hidden="true">✓</div>
        <h1 className="confirmation-title">Thank you — your order is placed!</h1>
        <p className="confirmation-text">
          We have sent a WhatsApp confirmation to your registered number. You will get another
          message at each stage of delivery.
        </p>

        <div className="confirmation-number">
          <span className="text-xs text-muted">Order number</span>
          <strong>{order.orderNumber}</strong>
        </div>

        <div className="confirmation-actions">
          <Link to={`/orders/${order.id}`} className="btn btn-primary btn-lg">Track this order</Link>
          <Link to="/products" className="btn btn-outline btn-lg">Continue shopping</Link>
        </div>
      </div>

      <div className="order-detail-grid mt-6">
        <section className="card">
          <div className="card-header">Order items ({order.items.length})</div>
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
              <span>Total paid</span>
              <span>{formatCurrency(order.totalAmount)}</span>
            </div>
          </div>
        </section>

        <section className="card">
          <div className="card-header">Delivery details</div>
          <div className="card-body">
            <dl className="detail-list">
              <div>
                <dt>Placed on</dt>
                <dd>{formatDateTime(order.placedAt)}</dd>
              </div>
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
              <div>
                <dt>Status</dt>
                <dd><span className="badge badge-warning">{order.statusLabel}</span></dd>
              </div>
            </dl>
          </div>
        </section>
      </div>
    </div>
  )
}
