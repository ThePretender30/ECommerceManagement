import { formatDateTime } from '../utils/format'
import './OrderStatusTimeline.css'

/**
 * The order tracking progress bar.
 *
 * The backend decides which steps exist and whether each is completed, current
 * or pending — including returning a short two-step timeline for a cancelled
 * order rather than a stalled progress bar. This component only renders what it
 * is given, so the delivery sequence is defined in exactly one place.
 */
export default function OrderStatusTimeline({ steps, cancelled }) {
  if (!steps?.length) return null

  return (
    <ol className={cancelled ? 'timeline is-cancelled' : 'timeline'}>
      {steps.map((step, index) => (
        <li key={step.status} className={`timeline-step is-${step.state}`}>
          <div className="timeline-marker">
            <span className="timeline-dot" aria-hidden="true">
              {step.state === 'completed' ? '✓' : index + 1}
            </span>
            {index < steps.length - 1 && <span className="timeline-line" aria-hidden="true" />}
          </div>

          <div className="timeline-content">
            <span className="timeline-label">{step.label}</span>
            {step.occurredAt ? (
              <time className="timeline-time" dateTime={step.occurredAt}>
                {formatDateTime(step.occurredAt)}
              </time>
            ) : (
              <span className="timeline-time is-pending">Pending</span>
            )}
          </div>
        </li>
      ))}
    </ol>
  )
}
