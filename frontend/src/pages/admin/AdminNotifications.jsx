import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState, ErrorState, Loader, Pagination } from '../../components/Common'
import { MessageIcon } from '../../components/Icons'
import adminService from '../../services/adminService'
import { formatDateTime } from '../../utils/format'

const STATUS_FILTERS = [
  { value: '', label: 'All notifications' },
  { value: 'SENT', label: 'Sent' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'SKIPPED', label: 'Skipped' },
]

export default function AdminNotifications() {
  const [result, setResult] = useState(null)
  const [summary, setSummary] = useState(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)

    Promise.all([
      adminService.listNotifications({ status: status || undefined, page, size: 20 }),
      adminService.notificationSummary().catch(() => null),
    ])
      .then(([list, counts]) => {
        setResult(list)
        setSummary(counts)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [status, page])
  useEffect(() => setPage(0), [status])

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Notifications</h1>
          <p className="admin-subtitle">Every WhatsApp message the system has attempted to send.</p>
        </div>
        <button type="button" className="btn btn-outline btn-sm" onClick={load}>Refresh</button>
      </div>

      {summary && (
        <div className="stat-grid">
          <div className="stat-card is-success">
            <span className="stat-card-label">Sent</span>
            <span className="stat-card-value">{summary.SENT ?? 0}</span>
            <span className="stat-card-hint">Delivered to Twilio</span>
          </div>
          <div className="stat-card is-danger">
            <span className="stat-card-label">Failed</span>
            <span className="stat-card-value">{summary.FAILED ?? 0}</span>
            <span className="stat-card-hint">Rejected by the provider</span>
          </div>
          <div className="stat-card is-warning">
            <span className="stat-card-label">Skipped</span>
            <span className="stat-card-value">{summary.SKIPPED ?? 0}</span>
            <span className="stat-card-hint">Sending disabled or not configured</span>
          </div>
          <div className="stat-card">
            <span className="stat-card-label">Total</span>
            <span className="stat-card-value">
              {(summary.SENT ?? 0) + (summary.FAILED ?? 0) + (summary.SKIPPED ?? 0)}
            </span>
            <span className="stat-card-hint">All attempts recorded</span>
          </div>
        </div>
      )}

      {summary && summary.SKIPPED > 0 && summary.SENT === 0 && (
        <div className="alert alert-info">
          WhatsApp sending is currently inactive, so messages are being logged instead of delivered.
          Set <code>WHATSAPP_ENABLED=true</code> along with your Twilio credentials in{' '}
          <code>backend/.env</code> to send real messages.
        </div>
      )}

      <div className="admin-toolbar">
        <select
          className="form-control"
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          aria-label="Filter by outcome"
        >
          {STATUS_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>

      {loading && <Loader label="Loading notifications…" />}
      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && result?.empty && (
        <EmptyState
          icon={<MessageIcon size={48} />}
          title="No notifications yet"
          message="Messages are recorded here whenever an order is placed or its status changes."
        />
      )}

      {!loading && !error && result && !result.empty && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Order</th>
                  <th>Recipient</th>
                  <th>Trigger</th>
                  <th>Message</th>
                  <th>Outcome</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((notification) => (
                  <tr key={notification.id}>
                    <td className="text-sm text-muted" style={{ whiteSpace: 'nowrap' }}>
                      {formatDateTime(notification.createdAt)}
                    </td>
                    <td>
                      {notification.orderId ? (
                        <Link to={`/admin/orders/${notification.orderId}`} className="font-semibold">
                          {notification.orderNumber}
                        </Link>
                      ) : '—'}
                    </td>
                    <td>
                      <div className="text-sm">{notification.userName ?? '—'}</div>
                      <div className="text-xs text-subtle">{notification.recipient}</div>
                    </td>
                    <td className="text-xs">
                      {notification.triggerStatus?.replaceAll('_', ' ') ?? '—'}
                    </td>
                    <td className="text-xs text-muted" style={{ maxWidth: 360, whiteSpace: 'pre-line' }}>
                      {notification.message}
                    </td>
                    <td>
                      <span
                        className={
                          notification.status === 'SENT' ? 'badge badge-success'
                          : notification.status === 'FAILED' ? 'badge badge-danger'
                          : 'badge badge-neutral'
                        }
                        title={notification.errorMessage ?? notification.providerMessageId ?? undefined}
                      >
                        {notification.status}
                      </span>
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
