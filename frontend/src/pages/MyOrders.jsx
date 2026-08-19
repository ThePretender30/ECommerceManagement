import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState, ErrorState, Loader, Pagination } from '../components/Common'
import orderService from '../services/orderService'
import {
  formatCurrency,
  formatDate,
  handleImageError,
  statusBadgeClass,
  FALLBACK_IMAGE,
} from '../utils/format'
import './Orders.css'

/** The customer's order history. */
export default function MyOrders() {
  const [result, setResult] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)
    orderService
      .list(page, 10)
      .then(setResult)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [page])

  if (loading) return <Loader fullPage label="Loading your orders…" />
  if (error) return <ErrorState message={error} onRetry={load} />

  if (result?.empty) {
    return (
      <div className="page container">
        <EmptyState
          icon="📦"
          title="You have not placed any orders yet"
          message="Once you place an order it will appear here, and you can track it at every step."
          action={<Link to="/products" className="btn btn-primary btn-lg">Browse products</Link>}
        />
      </div>
    )
  }

  return (
    <div className="page container">
      <div className="page-header">
        <h1 className="page-title">My orders</h1>
        <p className="page-subtitle">
          {result.totalElements} {result.totalElements === 1 ? 'order' : 'orders'} placed
        </p>
      </div>

      <div className="order-list">
        {result.content.map((order) => (
          <Link key={order.id} to={`/orders/${order.id}`} className="order-row">
            <div className="order-row-image">
              <img
                src={order.firstItemImageUrl || FALLBACK_IMAGE}
                alt={order.firstItemName ?? 'Order'}
                onError={handleImageError}
              />
              {order.itemCount > 1 && (
                <span className="order-row-more">+{order.itemCount - 1}</span>
              )}
            </div>

            <div className="order-row-info">
              <span className="order-row-number">{order.orderNumber}</span>
              <span className="order-row-items">
                {order.firstItemName}
                {order.itemCount > 1 && ` and ${order.itemCount - 1} more`}
              </span>
              <span className="text-xs text-subtle">Placed on {formatDate(order.placedAt)}</span>
            </div>

            <div className="order-row-meta">
              <span className={`badge ${statusBadgeClass(order.status)}`}>{order.statusLabel}</span>
              <span className="order-row-total">{formatCurrency(order.totalAmount)}</span>
            </div>
          </Link>
        ))}
      </div>

      <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />
    </div>
  )
}
