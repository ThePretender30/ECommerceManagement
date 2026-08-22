import './Toast.css'

const ICONS = {
  success: '✓',
  error: '!',
  warning: '!',
  info: 'i',
}

export default function Toast({ toasts, onDismiss }) {
  if (!toasts.length) return null

  return (
    <div className="toast-stack" role="status" aria-live="polite">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast-${toast.type}`}>
          <span className="toast-icon" aria-hidden="true">{ICONS[toast.type] ?? 'i'}</span>
          <p className="toast-message">{toast.message}</p>
          <button
            type="button"
            className="toast-close"
            onClick={() => onDismiss(toast.id)}
            aria-label="Dismiss notification"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )
}
