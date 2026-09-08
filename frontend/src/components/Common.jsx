import { Link } from 'react-router-dom'
import { AlertTriangleIcon, PackageIcon } from './Icons'
import './Common.css'

export function Loader({ label = 'Loading…', fullPage = false }) {
  return (
    <div className={fullPage ? 'loader loader-full' : 'loader'} role="status">
      <span className="loader-spinner" aria-hidden="true" />
      <span className="sr-only">{label}</span>
    </div>
  )
}

export function EmptyState({ icon, title, message, action }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon" aria-hidden="true">
        {icon ?? <PackageIcon size={40} />}
      </div>
      <h3 className="empty-state-title">{title}</h3>
      {message && <p className="empty-state-message">{message}</p>}
      {action && <div className="empty-state-action">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="error-state">
      <div className="error-state-icon" aria-hidden="true">
        <AlertTriangleIcon size={40} />
      </div>
      <p className="error-state-message">{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-outline btn-sm mt-4" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}

export function StarRating({ value = 0, count, size = 'md', showValue = true }) {
  const rating = Math.max(0, Math.min(5, Number(value) || 0))
  const percent = (rating / 5) * 100

  return (
    <span className={`stars stars-${size}`} title={`${rating.toFixed(1)} out of 5`}>
      <span className="stars-track" aria-hidden="true">
        <span className="stars-empty">★★★★★</span>
        <span className="stars-filled" style={{ width: `${percent}%` }}>★★★★★</span>
      </span>
      {showValue && <span className="stars-value">{rating.toFixed(1)}</span>}
      {count !== undefined && <span className="stars-count">({count})</span>}
      <span className="sr-only">{rating.toFixed(1)} out of 5 stars</span>
    </span>
  )
}

export function QuantityStepper({ value, onChange, min = 1, max = 99, disabled = false }) {
  const decrease = () => onChange(Math.max(min, value - 1))
  const increase = () => onChange(Math.min(max, value + 1))

  return (
    <div className="qty-stepper">
      <button
        type="button"
        onClick={decrease}
        disabled={disabled || value <= min}
        aria-label="Decrease quantity"
      >
        −
      </button>
      <span className="qty-value" aria-live="polite">{value}</span>
      <button
        type="button"
        onClick={increase}
        disabled={disabled || value >= max}
        aria-label="Increase quantity"
      >
        +
      </button>
    </div>
  )
}

export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  const windowSize = 5
  let start = Math.max(0, page - Math.floor(windowSize / 2))
  const end = Math.min(totalPages, start + windowSize)
  start = Math.max(0, end - windowSize)

  const pages = Array.from({ length: end - start }, (_, i) => start + i)

  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="pagination-btn"
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
      >
        ‹ Prev
      </button>

      {start > 0 && (
        <>
          <button type="button" className="pagination-btn" onClick={() => onPageChange(0)}>1</button>
          {start > 1 && <span className="pagination-gap">…</span>}
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          className={p === page ? 'pagination-btn is-active' : 'pagination-btn'}
          onClick={() => onPageChange(p)}
          aria-current={p === page ? 'page' : undefined}
        >
          {p + 1}
        </button>
      ))}

      {end < totalPages && (
        <>
          {end < totalPages - 1 && <span className="pagination-gap">…</span>}
          <button
            type="button"
            className="pagination-btn"
            onClick={() => onPageChange(totalPages - 1)}
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        type="button"
        className="pagination-btn"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
      >
        Next ›
      </button>
    </nav>
  )
}

export function Breadcrumbs({ items }) {
  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      {items.map((item, index) => (
        <span key={`${item.label}-${index}`}>
          {item.to ? <Link to={item.to}>{item.label}</Link> : <span aria-current="page">{item.label}</span>}
          {index < items.length - 1 && <span className="breadcrumbs-sep" aria-hidden="true">›</span>}
        </span>
      ))}
    </nav>
  )
}
