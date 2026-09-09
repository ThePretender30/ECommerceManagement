import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth, useToast } from '../../hooks'
import { ShieldCheckIcon } from '../../components/Icons'
import '../Auth.css'

export default function AdminLogin() {
  const { loginAdmin } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [form, setForm] = useState({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      const user = await loginAdmin(form.email, form.password)
      toast.success(`Welcome to Admin Panel, ${user.fullName.split(' ')[0]}!`)

      const redirect = searchParams.get('redirect')
      navigate(redirect ? decodeURIComponent(redirect) : '/admin', { replace: true })
    } catch (err) {
      setError(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page auth-page-admin">
      <div className="auth-card auth-card-admin">
        <div className="auth-header">
          <div className="auth-badge">
            <ShieldCheckIcon size={14} /> Admin Portal
          </div>
          <h1 className="auth-title">Administrator Sign In</h1>
          <p className="auth-subtitle">Authorized store management personnel only.</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="adminEmail">Admin Email</label>
            <input
              id="adminEmail"
              name="email"
              type="email"
              className={fieldErrors.email ? 'form-control has-error' : 'form-control'}
              value={form.email}
              onChange={handleChange}
              placeholder="admin@ecommerce.local"
              autoComplete="email"
              required
            />
            {fieldErrors.email && <span className="form-error">{fieldErrors.email}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="adminPassword">Admin Password</label>
            <input
              id="adminPassword"
              name="password"
              type="password"
              className={fieldErrors.password ? 'form-control has-error' : 'form-control'}
              value={form.password}
              onChange={handleChange}
              placeholder="Your administrator password"
              autoComplete="current-password"
              required
            />
            {fieldErrors.password && <span className="form-error">{fieldErrors.password}</span>}
          </div>

          <button type="submit" className="btn btn-primary btn-block btn-lg mt-4" disabled={submitting}>
            {submitting ? 'Verifying admin credentials…' : 'Sign in as Administrator'}
          </button>
        </form>

        <div className="auth-portal-switch">
          Customer looking to shop? <Link to="/login">Customer Sign In →</Link>
        </div>

        <p className="auth-footer">
          <Link to="/">← Return to Storefront</Link>
        </p>
      </div>
    </div>
  )
}
