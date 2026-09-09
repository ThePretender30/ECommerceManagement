import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth, useToast } from '../hooks'
import './Auth.css'

export default function Login() {
  const { login } = useAuth()
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
      const user = await login(form.email, form.password)
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}!`)

      const redirect = searchParams.get('redirect')
      if (redirect) {
        navigate(decodeURIComponent(redirect), { replace: true })
      } else {
        navigate(user.roles?.includes('ROLE_ADMIN') ? '/admin' : '/', { replace: true })
      }
    } catch (err) {
      setError(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <h1 className="auth-title">Customer Sign In</h1>
          <p className="auth-subtitle">Sign in to your Roz Bazaar account.</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="email">Email address</label>
            <input
              id="email"
              name="email"
              type="email"
              className={fieldErrors.email ? 'form-control has-error' : 'form-control'}
              value={form.email}
              onChange={handleChange}
              placeholder="you@example.com"
              autoComplete="email"
              required
            />
            {fieldErrors.email && <span className="form-error">{fieldErrors.email}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              className={fieldErrors.password ? 'form-control has-error' : 'form-control'}
              value={form.password}
              onChange={handleChange}
              placeholder="Your password"
              autoComplete="current-password"
              required
            />
            {fieldErrors.password && <span className="form-error">{fieldErrors.password}</span>}
          </div>

          <button type="submit" className="btn btn-primary btn-block btn-lg mt-4" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="auth-portal-switch">
          Are you a store administrator? <Link to="/admin/login">Admin Sign In →</Link>
        </div>

        <p className="auth-footer">
          Don&apos;t have an account? <Link to="/register">Create one</Link>
        </p>
      </div>
    </div>
  )
}
