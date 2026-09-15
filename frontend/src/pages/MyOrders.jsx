import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { EmptyState, ErrorState, Loader, Pagination } from '../components/Common'
import { CartIcon, PackageIcon } from '../components/Icons'
import { useCart, useToast } from '../hooks'
import orderService from '../services/orderService'
import {
  formatCurrency,
  formatDate,
  handleImageError,
  statusBadgeClass,
  FALLBACK_IMAGE,
} from '../utils/format'
import './Orders.css'

export default function MyOrders() {
  const [result, setResult] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [reorderingId, setReorderingId] = useState(null)

  const { addItem } = useCart()
  const toast = useToast()
  const navigate = useNavigate()

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

  const handleBuyAgain = async (event, order) => {
    event.preventDefault()
    event.stopPropagation()

    setReorderingId(order.id)
    let addedCount = 0
    let failedCount = 0

    try {
      // Summary response does not contain items array; fetch full order details
      const fullOrder = await orderService.getById(order.id)
      const items = fullOrder?.items || []

      if (items.length === 0) {
        toast.info('No items found to reorder.')
        return
      }

      for (const item of items) {
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
      } else {
        toast.info('No available items to reorder.')
      }
    } catch (err) {
      toast.error(err.message || 'Failed to reorder items.')
    } finally {
      setReorderingId(null)
    }
  }

  if (loading) return <Loader fullPage label="Loading your orders…" />
  if (error) return <ErrorState message={error} onRetry={load} />

  if (result?.empty) {
    return (
      <div className="page container">
        <EmptyState
          icon={<PackageIcon size={48} />}
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
              <button
                type="button"
                className="btn btn-outline btn-sm order-row-reorder"
                onClick={(e) => handleBuyAgain(e, order)}
                disabled={reorderingId === order.id}
                title="Add all items from this order back to your cart"
              >
                <CartIcon size={14} />
                {reorderingId === order.id ? 'Adding…' : 'Buy again'}
              </button>
            </div>
          </Link>
        ))}
      </div>

      <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />
    </div>
  )
}
