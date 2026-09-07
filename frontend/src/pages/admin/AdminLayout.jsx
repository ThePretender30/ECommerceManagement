import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks'
import './Admin.css'

const NAV_ITEMS = [
  { to: '/admin', label: 'Dashboard', icon: '📊', end: true },
  { to: '/admin/products', label: 'Products', icon: '📦' },
  { to: '/admin/categories', label: 'Categories', icon: '🗂️' },
  { to: '/admin/orders', label: 'Orders', icon: '🧾' },
  { to: '/admin/users', label: 'Users', icon: '👥' },
  { to: '/admin/notifications', label: 'Notifications', icon: '💬' },
]

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <div className="admin">
      {sidebarOpen && (
        <div className="admin-overlay" onClick={() => setSidebarOpen(false)} aria-hidden="true" />
      )}

      <aside className={sidebarOpen ? 'admin-sidebar is-open' : 'admin-sidebar'}>
        <div className="admin-brand">
          <span className="admin-brand-mark" aria-hidden="true">रोज़</span>
          <div>
            <strong>Roz Bazaar</strong>
            <span className="admin-brand-sub">Admin</span>
          </div>
        </div>

        <nav className="admin-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className="admin-nav-link"
              onClick={() => setSidebarOpen(false)}
            >
              <span aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="admin-sidebar-footer">
          <Link to="/" className="admin-nav-link">
            <span aria-hidden="true">🏬</span>
            View storefront
          </Link>
          <button type="button" className="admin-nav-link is-danger" onClick={handleLogout}>
            <span aria-hidden="true">🚪</span>
            Sign out
          </button>
        </div>
      </aside>

      <div className="admin-main">
        <header className="admin-topbar">
          <button
            type="button"
            className="admin-burger"
            onClick={() => setSidebarOpen((open) => !open)}
            aria-label="Toggle admin menu"
          >
            ☰
          </button>

          <div className="admin-topbar-user">
            <span className="admin-avatar" aria-hidden="true">
              {user?.fullName?.charAt(0)?.toUpperCase() ?? 'A'}
            </span>
            <div className="admin-topbar-details">
              <strong className="text-sm">{user?.fullName}</strong>
              <span className="text-xs text-muted">Administrator</span>
            </div>
          </div>
        </header>

        <div className="admin-content">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
