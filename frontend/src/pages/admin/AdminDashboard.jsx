import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState, Loader } from '../../components/Common'
import adminService from '../../services/adminService'
import { formatCurrency, formatDate, statusBadgeClass } from '../../utils/format'

const STATUS_LABELS = {
  ORDER_PLACED: 'Order Placed',
  ORDER_CONFIRMED: 'Order Confirmed',
  PROCESSING: 'Processing',
  DISPATCHED: 'Dispatched',
  OUT_FOR_DELIVERY: 'Out for Delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)
    adminService
      .stats()
      .then(setStats)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  if (loading) return <Loader fullPage label="Loading dashboard…" />
  if (error) return <ErrorState message={error} onRetry={load} />
  if (!stats) return null

  const maxStatusCount = Math.max(...Object.values(stats.ordersByStatus), 1)
  const maxUnitsSold = Math.max(...stats.topSellingProducts.map((p) => p.unitsSold), 1)

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Dashboard</h1>
          <p className="admin-subtitle">An overview of sales, orders and inventory.</p>
        </div>
        <button type="button" className="btn btn-outline btn-sm" onClick={load}>
          Refresh
        </button>
      </div>

      <div className="stat-grid">
        <div className="stat-card is-success">
          <span className="stat-card-label">Total revenue</span>
          <span className="stat-card-value">{formatCurrency(stats.totalRevenue)}</span>
          <span className="stat-card-hint">
            {formatCurrency(stats.revenueLast30Days)} in the last 30 days
          </span>
        </div>

        <div className="stat-card is-primary">
          <span className="stat-card-label">Total orders</span>
          <span className="stat-card-value">{stats.totalOrders}</span>
          <span className="stat-card-hint">{stats.ordersLast30Days} in the last 30 days</span>
        </div>

        <div className="stat-card is-warning">
          <span className="stat-card-label">Pending orders</span>
          <span className="stat-card-value">{stats.pendingOrders}</span>
          <span className="stat-card-hint">Awaiting delivery</span>
        </div>

        <div className="stat-card">
          <span className="stat-card-label">Customers</span>
          <span className="stat-card-value">{stats.totalCustomers}</span>
          <span className="stat-card-hint">{stats.newCustomersLast30Days} joined recently</span>
        </div>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <span className="stat-card-label">Active products</span>
          <span className="stat-card-value">{stats.totalProducts}</span>
          <span className="stat-card-hint">Across {stats.totalCategories} categories</span>
        </div>

        <div className={stats.lowStockCount > 0 ? 'stat-card is-danger' : 'stat-card'}>
          <span className="stat-card-label">Low stock</span>
          <span className="stat-card-value">{stats.lowStockCount}</span>
          <span className="stat-card-hint">Needs restocking</span>
        </div>

        <div className="stat-card is-success">
          <span className="stat-card-label">Delivered</span>
          <span className="stat-card-value">{stats.deliveredOrders}</span>
          <span className="stat-card-hint">Completed orders</span>
        </div>

        <div className="stat-card is-danger">
          <span className="stat-card-label">Cancelled</span>
          <span className="stat-card-value">{stats.cancelledOrders}</span>
          <span className="stat-card-hint">Stock returned automatically</span>
        </div>
      </div>

      <div className="admin-grid cols-2 mt-6">
        <section className="card">
          <div className="card-header">Orders by status</div>
          <div className="card-body">
            <div className="bar-list">
              {Object.entries(stats.ordersByStatus).map(([status, count]) => (
                <div key={status} className="bar-row">
                  <span>{STATUS_LABELS[status] ?? status}</span>
                  <span className="font-semibold">{count}</span>
                  <span className="bar-track">
                    <span
                      className={
                        status === 'DELIVERED' ? 'bar-fill is-success'
                        : status === 'CANCELLED' ? 'bar-fill is-danger'
                        : 'bar-fill'
                      }
                      style={{ width: `${(count / maxStatusCount) * 100}%` }}
                    />
                  </span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="card">
          <div className="card-header">Best sellers</div>
          <div className="card-body">
            {stats.topSellingProducts.length === 0 ? (
              <p className="text-sm text-muted">
                No sales yet. Best sellers appear here once orders are placed.
              </p>
            ) : (
              <div className="bar-list">
                {stats.topSellingProducts.map((product) => (
                  <div key={product.productId} className="bar-row">
                    <span>{product.productName}</span>
                    <span className="font-semibold">
                      {product.unitsSold} sold · {formatCurrency(product.revenue)}
                    </span>
                    <span className="bar-track">
                      <span
                        className="bar-fill is-accent"
                        style={{ width: `${(product.unitsSold / maxUnitsSold) * 100}%` }}
                      />
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>

      <div className="admin-grid split mt-6">
        <section className="card">
          <div className="card-header">
            Recent orders
            <Link to="/admin/orders" className="text-sm">View all</Link>
          </div>
          <div className="table-wrap" style={{ border: 'none', borderRadius: 0 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer</th>
                  <th>Status</th>
                  <th className="text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {stats.recentOrders.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="text-center text-muted">No orders yet.</td>
                  </tr>
                ) : (
                  stats.recentOrders.map((order) => (
                    <tr key={order.id}>
                      <td>
                        <Link to={`/admin/orders/${order.id}`} className="font-semibold">
                          {order.orderNumber}
                        </Link>
                        <div className="text-xs text-subtle">{formatDate(order.placedAt)}</div>
                      </td>
                      <td>
                        <div className="text-sm">{order.customerName}</div>
                        <div className="text-xs text-subtle">{order.customerEmail}</div>
                      </td>
                      <td>
                        <span className={`badge ${statusBadgeClass(order.status)}`}>
                          {order.statusLabel}
                        </span>
                      </td>
                      <td className="text-right font-semibold">
                        {formatCurrency(order.totalAmount)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card">
          <div className="card-header">Low stock
            <Link to="/admin/products" className="text-sm">Manage</Link>
          </div>
          <div className="card-body">
            {stats.lowStockProducts.length === 0 ? (
              <p className="text-sm text-muted">
                Everything is well stocked. Products with 5 or fewer units appear here.
              </p>
            ) : (
              <ul className="stack">
                {stats.lowStockProducts.map((product) => (
                  <li key={product.productId} className="row-between">
                    <div>
                      <Link to={`/admin/products/${product.productId}/edit`} className="text-sm font-semibold">
                        {product.productName}
                      </Link>
                      <div className="text-xs text-subtle">{product.categoryName}</div>
                    </div>
                    <span className={product.stock === 0 ? 'stock-pill is-none' : 'stock-pill is-low'}>
                      {product.stock} left
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
