import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CartIcon, MapPinIcon, PackageIcon } from '../components/Icons'
import authService from '../services/authService'
import { useAuth, useToast } from '../hooks'
import { formatDate } from '../utils/format'
import './Profile.css'

function parsePhone(rawPhone) {
  if (!rawPhone) return { countryCode: '+91', mobileNumber: '' }
  const trimmed = rawPhone.trim()
  const match = trimmed.match(/^(\+[1-9]\d{0,3})(\d{9,10})$/)
  if (match) {
    return { countryCode: match[1], mobileNumber: match[2] }
  }
  if (trimmed.startsWith('+91') && trimmed.length > 3) {
    return { countryCode: '+91', mobileNumber: trimmed.slice(3).replace(/\D/g, '').slice(0, 10) }
  }
  if (trimmed.startsWith('+')) {
    return { countryCode: trimmed.slice(0, 3), mobileNumber: trimmed.slice(3).replace(/\D/g, '').slice(0, 10) }
  }
  return { countryCode: '+91', mobileNumber: trimmed.replace(/\D/g, '').slice(0, 10) }
}

export default function Profile() {
  const { user, updateUser } = useAuth()
  const toast = useToast()

  const initialPhone = parsePhone(user?.phoneNumber)
  const [profile, setProfile] = useState({
    fullName: user?.fullName ?? '',
    countryCode: initialPhone.countryCode,
    mobileNumber: initialPhone.mobileNumber,
  })
  const [profileErrors, setProfileErrors] = useState({})
  const [savingProfile, setSavingProfile] = useState(false)

  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [passwordErrors, setPasswordErrors] = useState({})
  const [savingPassword, setSavingPassword] = useState(false)

  const handleCountryCodeChange = (event) => {
    let val = event.target.value.trim()
    if (!val) {
      val = '+'
    } else if (!val.startsWith('+')) {
      val = '+' + val.replace(/\D/g, '')
    } else {
      val = '+' + val.slice(1).replace(/\D/g, '')
    }
    val = val.slice(0, 5)
    setProfile((p) => ({ ...p, countryCode: val }))
    setProfileErrors((p) => ({ ...p, countryCode: undefined, phoneNumber: undefined }))
  }

  const handleMobileNumberChange = (event) => {
    const digitsOnly = event.target.value.replace(/\D/g, '').slice(0, 10)
    setProfile((p) => ({ ...p, mobileNumber: digitsOnly }))
    setProfileErrors((p) => ({ ...p, mobileNumber: undefined, phoneNumber: undefined }))
  }

  const handleProfileSubmit = async (event) => {
    event.preventDefault()
    setProfileErrors({})
    const nameRegex = /^[a-zA-Z]+(?: [a-zA-Z]+)*$/
    if (!nameRegex.test(profile.fullName.trim())) {
      setProfileErrors({ fullName: 'Full name can only contain letters and spaces (no dots, digits, or symbols).' })
      return
    }

    const countryCodeRegex = /^\+[1-9]\d{0,3}$/
    if (!countryCodeRegex.test(profile.countryCode.trim())) {
      setProfileErrors({ countryCode: 'Enter a valid country code (e.g. +91, +1, +44).' })
      return
    }

    const mobileRegex = /^\d{9,10}$/
    if (!mobileRegex.test(profile.mobileNumber.trim())) {
      setProfileErrors({ mobileNumber: 'Mobile number must be 9 or 10 digits only (numbers only).' })
      return
    }

    setSavingProfile(true)

    try {
      const fullPhoneNumber = profile.countryCode.trim() + profile.mobileNumber.trim()
      const updated = await authService.updateProfile({
        fullName: profile.fullName,
        phoneNumber: fullPhoneNumber,
      })
      updateUser(updated)
      toast.success('Your profile has been updated.')
    } catch (err) {
      toast.error(err.message)
      if (err.fieldErrors) setProfileErrors(err.fieldErrors)
    } finally {
      setSavingProfile(false)
    }
  }

  const handlePasswordSubmit = async (event) => {
    event.preventDefault()
    setPasswordErrors({})

    if (passwords.newPassword !== passwords.confirmPassword) {
      setPasswordErrors({ confirmPassword: 'Passwords do not match.' })
      return
    }

    setSavingPassword(true)
    try {
      await authService.changePassword(passwords.currentPassword, passwords.newPassword)
      toast.success('Your password has been changed.')
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      toast.error(err.message)
      if (err.fieldErrors) setPasswordErrors(err.fieldErrors)
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <div className="page container">
      <div className="page-header">
        <h1 className="page-title">My profile</h1>
        <p className="page-subtitle">Manage your account details and password.</p>
      </div>

      <div className="profile-layout">
        <aside className="profile-sidebar">
          <div className="profile-card">
            <span className="profile-avatar" aria-hidden="true">
              {user?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
            </span>
            <h2 className="profile-name">{user?.fullName}</h2>
            <p className="profile-email">{user?.email}</p>

            <div className="profile-roles">
              {user?.roles?.map((role) => (
                <span key={role} className="badge badge-primary">
                  {role.replace('ROLE_', '').toLowerCase()}
                </span>
              ))}
            </div>

            <p className="text-xs text-subtle mt-4">
              Member since {formatDate(user?.createdAt)}
            </p>
          </div>

          <nav className="profile-links">
            <Link to="/orders" className="profile-link">
              <PackageIcon size={18} /> My Orders
            </Link>
            <Link to="/addresses" className="profile-link">
              <MapPinIcon size={18} /> Delivery Addresses
            </Link>
            <Link to="/cart" className="profile-link">
              <CartIcon size={18} /> Shopping Cart
            </Link>
          </nav>
        </aside>

        <div className="stack">
          <section className="card">
            <div className="card-header">Account details</div>
            <div className="card-body">
              <form onSubmit={handleProfileSubmit} noValidate>
                <div className="form-group">
                  <label className="form-label" htmlFor="fullName">Full name</label>
                  <input
                    id="fullName"
                    type="text"
                    className={profileErrors.fullName ? 'form-control has-error' : 'form-control'}
                    value={profile.fullName}
                    onChange={(e) => setProfile((p) => ({ ...p, fullName: e.target.value }))}
                    required
                  />
                  {profileErrors.fullName && (
                    <span className="form-error">{profileErrors.fullName}</span>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="profileEmail">Email address</label>
                  <input
                    id="profileEmail"
                    type="email"
                    className="form-control"
                    value={user?.email ?? ''}
                    disabled
                  />
                  <span className="form-hint">
                    Your email is your sign-in identity and cannot be changed here.
                  </span>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="mobileNumber">WhatsApp number</label>
                  <div className="phone-input-group">
                    <input
                      id="countryCode"
                      name="countryCode"
                      type="text"
                      className={profileErrors.countryCode ? 'form-control country-code-input has-error' : 'form-control country-code-input'}
                      value={profile.countryCode}
                      onChange={handleCountryCodeChange}
                      placeholder="+91"
                      maxLength={5}
                      title="Country calling code (e.g. +91, +1, +44)"
                      required
                    />
                    <input
                      id="mobileNumber"
                      name="mobileNumber"
                      type="tel"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={10}
                      className={profileErrors.mobileNumber || profileErrors.phoneNumber ? 'form-control mobile-number-input has-error' : 'form-control mobile-number-input'}
                      value={profile.mobileNumber}
                      onChange={handleMobileNumberChange}
                      placeholder="9876543210 (9-10 digits)"
                      autoComplete="tel-national"
                      required
                    />
                  </div>
                  {profileErrors.countryCode && (
                    <span className="form-error">{profileErrors.countryCode}</span>
                  )}
                  {profileErrors.mobileNumber && (
                    <span className="form-error">{profileErrors.mobileNumber}</span>
                  )}
                  {!profileErrors.countryCode && !profileErrors.mobileNumber && profileErrors.phoneNumber && (
                    <span className="form-error">{profileErrors.phoneNumber}</span>
                  )}
                  {!profileErrors.countryCode && !profileErrors.mobileNumber && !profileErrors.phoneNumber && (
                    <span className="form-hint">Separate country code (e.g. +91) and 9 or 10-digit mobile number.</span>
                  )}
                </div>

                <button type="submit" className="btn btn-primary" disabled={savingProfile}>
                  {savingProfile ? 'Saving…' : 'Save changes'}
                </button>
              </form>
            </div>
          </section>

          <section className="card">
            <div className="card-header">Change password</div>
            <div className="card-body">
              <form onSubmit={handlePasswordSubmit} noValidate>
                <div className="form-group">
                  <label className="form-label" htmlFor="currentPassword">Current password</label>
                  <input
                    id="currentPassword"
                    type="password"
                    className={passwordErrors.currentPassword ? 'form-control has-error' : 'form-control'}
                    value={passwords.currentPassword}
                    onChange={(e) => setPasswords((p) => ({ ...p, currentPassword: e.target.value }))}
                    autoComplete="current-password"
                    required
                  />
                  {passwordErrors.currentPassword && (
                    <span className="form-error">{passwordErrors.currentPassword}</span>
                  )}
                </div>

                <div className="form-row cols-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="newPassword">New password</label>
                    <input
                      id="newPassword"
                      type="password"
                      className={passwordErrors.newPassword ? 'form-control has-error' : 'form-control'}
                      value={passwords.newPassword}
                      onChange={(e) => setPasswords((p) => ({ ...p, newPassword: e.target.value }))}
                      placeholder="At least 8 characters"
                      autoComplete="new-password"
                      required
                    />
                    {passwordErrors.newPassword && (
                      <span className="form-error">{passwordErrors.newPassword}</span>
                    )}
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="confirmPassword">Confirm new password</label>
                    <input
                      id="confirmPassword"
                      type="password"
                      className={passwordErrors.confirmPassword ? 'form-control has-error' : 'form-control'}
                      value={passwords.confirmPassword}
                      onChange={(e) => setPasswords((p) => ({ ...p, confirmPassword: e.target.value }))}
                      autoComplete="new-password"
                      required
                    />
                    {passwordErrors.confirmPassword && (
                      <span className="form-error">{passwordErrors.confirmPassword}</span>
                    )}
                  </div>
                </div>

                <button type="submit" className="btn btn-primary" disabled={savingPassword}>
                  {savingPassword ? 'Updating…' : 'Change password'}
                </button>
              </form>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
