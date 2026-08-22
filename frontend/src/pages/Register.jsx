import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth, useToast } from '../hooks'
import './Auth.css'

export default function Register() {
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  })
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

    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match.' })
      return
    }

    setSubmitting(true)
    try {
      const user = await register({
        fullName: form.fullName,
        email: form.email,
        phoneNumber: form.phoneNumber,
        password: form.password,
      })
      toast.success(`Welcome to ShopSphere, ${user.fullName.split(' ')[0]}!`)

      const redirect = searchParams.get('redirect')
      navigate(redirect ? decodeURIComponent(redirect) : '/', { replace: true })
    } catch (err) {
      setError(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card auth-card-wide">
        <div className="auth-header">
          <h1 className="auth-title">Create your account</h1>
          <p className="auth-subtitle">It only takes a minute.</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="fullName">Full name</label>
            <input
              id="fullName"
              name="fullName"
              type="text"
              className={fieldErrors.fullName ? 'form-control has-error' : 'form-control'}
              value={form.fullName}
              onChange={handleChange}
              placeholder="Aisha Sharma"
              autoComplete="name"
              required
            />
            {fieldErrors.fullName && <span className="form-error">{fieldErrors.fullName}</span>}
          </div>

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
            <label className="form-label" htmlFor="phoneNumber">WhatsApp number</label>
            <input
              id="phoneNumber"
              name="phoneNumber"
              type="tel"
              className={fieldErrors.phoneNumber ? 'form-control has-error' : 'form-control'}
              value={form.phoneNumber}
              onChange={handleChange}
              placeholder="+919876543210"
              autoComplete="tel"
              required
            />
            {fieldErrors.phoneNumber ? (
              <span className="form-error">{fieldErrors.phoneNumber}</span>
            ) : (
              <span className="form-hint">
                Include your country code. We send order updates to this number.
              </span>
            )}
          </div>

          <div className="form-row cols-2">
            <div className="form-group">
              <label className="form-label" htmlFor="password">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                className={fieldErrors.password ? 'form-control has-error' : 'form-control'}
                value={form.password}
                onChange={handleChange}
                placeholder="At least 8 characters"
                autoComplete="new-password"
                required
              />
              {fieldErrors.password && <span className="form-error">{fieldErrors.password}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="confirmPassword">Confirm password</label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                className={fieldErrors.confirmPassword ? 'form-control has-error' : 'form-control'}
                value={form.confirmPassword}
                onChange={handleChange}
                placeholder="Re-enter your password"
                autoComplete="new-password"
                required
              />
              {fieldErrors.confirmPassword && (
                <span className="form-error">{fieldErrors.confirmPassword}</span>
              )}
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-block btn-lg mt-4" disabled={submitting}>
            {submitting ? 'Creating your account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
