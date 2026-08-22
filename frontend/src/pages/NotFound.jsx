import { Link } from 'react-router-dom'
import { EmptyState } from '../components/Common'

export default function NotFound() {
  return (
    <div className="page container">
      <EmptyState
        icon="🧭"
        title="Page not found"
        message="The page you are looking for does not exist, or may have moved."
        action={
          <div className="row wrap" style={{ justifyContent: 'center' }}>
            <Link to="/" className="btn btn-primary btn-lg">Go home</Link>
            <Link to="/products" className="btn btn-outline btn-lg">Browse products</Link>
          </div>
        }
      />
    </div>
  )
}
