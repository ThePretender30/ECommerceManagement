import { useState } from 'react'
import { Link } from 'react-router-dom'
import authService from '../services/authService'
import { useAuth, useToast } from '../hooks'
import { formatDate } from '../utils/format'
import './Profile.css'

/**
 * Account settings: name, WhatsApp number and password.
 *
 * Email is shown read-only because it is the login identity and the subject of
 * every issued token — changing it would need a re-verification flow, which is
 * out of scope here. The field explains that rather than silently disabling.
 */
export default function Profile() {
  const { user, updateUser } = useAuth()
  const toast = useToast()

  const [profile, setProfile] = useState({
    fullName: user?.fullName ?? '',
    phoneNumber: user?.phoneNumber ?? '',
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

  const handleProfileSubmit = async (event) => {
    event.preventDefault()
    setProfileErrors({})
    setSavingProfile(true)

    try {
      const updated = await authService.updateProfile(profile)
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
            <Link to="/orders" className="profile-link">📦 My Orders</Link>
            <Link to="/addresses" className="profile-link">📍 Delivery Addresses</Link>
            <Link to="/cart" className="profile-link">🛒 Shopping Cart</Link>
          </nav>
        </aside>

        <div className="stack">
          {/* ---------------- Profile details ---------------- */}
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
                  <label className="form-label" htmlFor="phoneNumber">WhatsApp number</label>
                  <input
                    id="phoneNumber"
                    type="tel"
                    className={profileErrors.phoneNumber ? 'form-control has-error' : 'form-control'}
                    value={profile.phoneNumber}
                    onChange={(e) => setProfile((p) => ({ ...p, phoneNumber: e.target.value }))}
                    placeholder="+919876543210"
                    required
                  />
                  {profileErrors.phoneNumber ? (
                    <span className="form-error">{profileErrors.phoneNumber}</span>
                  ) : (
                    <span className="form-hint">Order notifications are sent to this number.</span>
                  )}
                </div>

                <button type="submit" className="btn btn-primary" disabled={savingProfile}>
                  {savingProfile ? 'Saving…' : 'Save changes'}
                </button>
              </form>
            </div>
          </section>

          {/* ---------------- Password ---------------- */}
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
