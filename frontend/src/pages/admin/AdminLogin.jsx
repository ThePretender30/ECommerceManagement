import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth, useToast } from '../../hooks'
import { ShieldCheckIcon } from '../../components/Icons'
import '../Auth.css'

export default function AdminLogin() {
  const { initiateAdminLogin, verifyOtp, resendOtp } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [step, setStep] = useState('credentials') // 'credentials' | 'otp'
  const [form, setForm] = useState({ email: '', password: '' })
  const [otp, setOtp] = useState('')
  const [challenge, setChallenge] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [resending, setResending] = useState(false)
  const [countdown, setCountdown] = useState(0)

  useEffect(() => {
    if (countdown <= 0) return
    const timer = setInterval(() => setCountdown((c) => c - 1), 1000)
    return () => clearInterval(timer)
  }, [countdown])

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleCredentialsSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      const challengeResponse = await initiateAdminLogin(form.email, form.password)
      setChallenge(challengeResponse)
      setStep('otp')
      setCountdown(60)
      toast.info('Admin verification code sent to your email.')
    } catch (err) {
      setError(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  const handleOtpSubmit = async (event) => {
    event.preventDefault()
    if (!otp.trim()) {
      setFieldErrors({ otp: 'Please enter the 6-digit verification code' })
      return
    }

    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      const user = await verifyOtp(challenge.sessionToken, otp.trim())
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

  const handleResendOtp = async () => {
    if (countdown > 0 || resending) return
    setError(null)
    setResending(true)

    try {
      const freshChallenge = await resendOtp(challenge.sessionToken)
      setChallenge(freshChallenge)
      setCountdown(60)
      setOtp('')
      toast.success('A fresh verification code has been sent.')
    } catch (err) {
      setError(err.message)
    } finally {
      setResending(false)
    }
  }

  return (
    <div className="auth-page auth-page-admin">
      <div className="auth-card auth-card-admin">
        <div className="auth-header">
          <div className="auth-badge">
            <ShieldCheckIcon size={14} /> Admin Portal
          </div>
          <h1 className="auth-title">
            {step === 'credentials' ? 'Administrator Sign In' : 'Email Verification'}
          </h1>
          <p className="auth-subtitle">
            {step === 'credentials'
              ? 'Authorized store management personnel only.'
              : `Enter the 6-digit code sent to ${challenge?.maskedEmail || 'your email'}.`}
          </p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {step === 'credentials' ? (
          <form onSubmit={handleCredentialsSubmit} noValidate>
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
              {submitting ? 'Verifying admin credentials…' : 'Continue to Verification'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleOtpSubmit} noValidate>
            <div className="form-group otp-box-wrapper">
              <label className="form-label" htmlFor="adminOtp">6-Digit Admin Verification Code</label>
              <input
                id="adminOtp"
                name="otp"
                type="text"
                maxLength={6}
                pattern="\d*"
                autoFocus
                className={fieldErrors.otp ? 'form-control otp-input has-error' : 'form-control otp-input'}
                value={otp}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '')
                  setOtp(val)
                  setFieldErrors({})
                }}
                placeholder="000000"
                autoComplete="one-time-code"
                required
              />
              {fieldErrors.otp && <span className="form-error">{fieldErrors.otp}</span>}
            </div>

            <div className="otp-resend">
              <button
                type="button"
                className="btn btn-link btn-sm"
                onClick={() => {
                  setStep('credentials')
                  setOtp('')
                  setError(null)
                }}
              >
                ← Back to credentials
              </button>

              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={handleResendOtp}
                disabled={countdown > 0 || resending}
              >
                {countdown > 0 ? `Resend in ${countdown}s` : resending ? 'Resending…' : 'Resend code'}
              </button>
            </div>

            <button type="submit" className="btn btn-primary btn-block btn-lg mt-4" disabled={submitting}>
              {submitting ? 'Verifying code…' : 'Verify & Enter Admin Panel'}
            </button>
          </form>
        )}

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
