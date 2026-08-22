import { formatDateTime } from '../utils/format'
import './OrderStatusTimeline.css'

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
