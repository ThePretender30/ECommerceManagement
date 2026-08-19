import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState, ErrorState, Loader, Pagination } from '../../components/Common'
import adminService from '../../services/adminService'
import { formatCurrency, formatDateTime, statusBadgeClass } from '../../utils/format'

const STATUS_FILTERS = [
  { value: '', label: 'All statuses' },
  { value: 'ORDER_PLACED', label: 'Order Placed' },
  { value: 'ORDER_CONFIRMED', label: 'Order Confirmed' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'DISPATCHED', label: 'Dispatched' },
  { value: 'OUT_FOR_DELIVERY', label: 'Out for Delivery' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

/** All orders across every customer, filterable by status. */
export default function AdminOrders() {
  const [result, setResult] = useState(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)
    adminService
      .listOrders({ status: status || undefined, page, size: 20 })
      .then(setResult)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [status, page])
  useEffect(() => setPage(0), [status])

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Orders</h1>
          <p className="admin-subtitle">
            {result ? `${result.totalElements} orders` : 'Loading…'}
          </p>
        </div>
        <button type="button" className="btn btn-outline btn-sm" onClick={load}>Refresh</button>
      </div>

      <div className="admin-toolbar">
        <select
          className="form-control"
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          aria-label="Filter by status"
        >
          {STATUS_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>

      {loading && <Loader label="Loading orders…" />}
      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && result?.empty && (
        <EmptyState
          icon="🧾"
          title="No orders found"
          message={status ? 'No orders currently have that status.' : 'Orders will appear here once customers start buying.'}
        />
      )}

      {!loading && !error && result && !result.empty && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer</th>
                  <th>Placed</th>
                  <th>Items</th>
                  <th>Status</th>
                  <th className="text-right">Total</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((order) => (
                  <tr key={order.id}>
                    <td>
                      <Link to={`/admin/orders/${order.id}`} className="font-semibold">
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td>
                      <div className="text-sm">{order.customerName}</div>
                      <div className="text-xs text-subtle">{order.customerEmail}</div>
                    </td>
                    <td className="text-sm text-muted">{formatDateTime(order.placedAt)}</td>
                    <td className="text-sm">{order.itemCount}</td>
                    <td>
                      <span className={`badge ${statusBadgeClass(order.status)}`}>
                        {order.statusLabel}
                      </span>
                    </td>
                    <td className="text-right font-semibold">
                      {formatCurrency(order.totalAmount)}
                    </td>
                    <td>
                      <Link to={`/admin/orders/${order.id}`} className="btn btn-outline btn-sm">
                        Manage
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
