import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth, useToast } from '../hooks'
import './Auth.css'

export default function Register() {
  const { initiateRegister, verifyRegisterOtp, resendRegisterOtp } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [step, setStep] = useState('form') // 'form' | 'otp'
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  })
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

  const handleFormSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})

    const nameRegex = /^[a-zA-Z]+(?: [a-zA-Z]+)*$/
    if (!nameRegex.test(form.fullName.trim())) {
      setFieldErrors({ fullName: 'Full name can only contain letters and spaces (no dots, digits, or symbols).' })
      return
    }

    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match.' })
      return
    }

    setSubmitting(true)
    try {
      const challengeResponse = await initiateRegister({
        fullName: form.fullName,
        email: form.email,
        phoneNumber: form.phoneNumber,
        password: form.password,
      })
      setChallenge(challengeResponse)
      setStep('otp')
      setCountdown(60)
      toast.info('Verification code sent to your email.')
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
      const user = await verifyRegisterOtp(challenge.sessionToken, otp.trim())
      toast.success(`Welcome to Roz Bazaar, ${user.fullName.split(' ')[0]}!`)

      const redirect = searchParams.get('redirect')
      navigate(redirect ? decodeURIComponent(redirect) : '/', { replace: true })
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
      const freshChallenge = await resendRegisterOtp(challenge.sessionToken)
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
    <div className="auth-page">
      <div className="auth-card auth-card-wide">
        <div className="auth-header">
          <h1 className="auth-title">
            {step === 'form' ? 'Create your account' : 'Verify your email'}
          </h1>
          <p className="auth-subtitle">
            {step === 'form'
              ? 'It only takes a minute.'
              : `Enter the 6-digit verification code sent to ${challenge?.maskedEmail || 'your email'}.`}
          </p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {step === 'form' ? (
          <form onSubmit={handleFormSubmit} noValidate>
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
              {submitting ? 'Sending verification code…' : 'Continue'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleOtpSubmit} noValidate>
            <div className="form-group otp-box-wrapper">
              <label className="form-label" htmlFor="registerOtp">6-Digit Verification Code</label>
              <input
                id="registerOtp"
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
                  setStep('form')
                  setOtp('')
                  setError(null)
                }}
              >
                ← Back to signup form
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
              {submitting ? 'Verifying code…' : 'Verify & Create Account'}
            </button>
          </form>
        )}

        <p className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
