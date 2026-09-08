import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth, useCart } from '../hooks'
import { CartIcon, SearchIcon } from './Icons'
import './Navbar.css'

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth()
  const { itemCount } = useCart()
  const navigate = useNavigate()
  const location = useLocation()

  const [query, setQuery] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const [accountOpen, setAccountOpen] = useState(false)
  const accountRef = useRef(null)

  useEffect(() => {
    setMenuOpen(false)
    setAccountOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!accountOpen) return
    const handleClick = (event) => {
      if (accountRef.current && !accountRef.current.contains(event.target)) {
        setAccountOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [accountOpen])

  const handleSearch = (event) => {
    event.preventDefault()
    const trimmed = query.trim()
    navigate(trimmed ? `/products?q=${encodeURIComponent(trimmed)}` : '/products')
    setMenuOpen(false)
  }

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <button
          type="button"
          className="navbar-burger"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-label="Toggle navigation menu"
        >
          <span /><span /><span />
        </button>

        <Link to="/" className="navbar-brand">
          <span className="navbar-brand-mark" aria-hidden="true">रोज़</span>
          <span className="navbar-brand-text">Bazaar</span>
        </Link>

        <form className="navbar-search" onSubmit={handleSearch} role="search">
          <input
            type="search"
            className="navbar-search-input"
            placeholder="Search products, brands and categories…"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            aria-label="Search products"
          />
          <button type="submit" className="navbar-search-btn" aria-label="Search">
            <SearchIcon size={18} />
          </button>
        </form>

        <nav className={menuOpen ? 'navbar-links is-open' : 'navbar-links'}>
          <NavLink to="/" end className="navbar-link">Home</NavLink>
          <NavLink to="/categories" className="navbar-link">Categories</NavLink>
          <NavLink to="/products" className="navbar-link">All Products</NavLink>
          {isAuthenticated && <NavLink to="/orders" className="navbar-link">My Orders</NavLink>}
          {isAdmin && <NavLink to="/admin" className="navbar-link navbar-link-admin">Admin</NavLink>}
        </nav>

        <div className="navbar-actions">
          {isAuthenticated ? (
            <div className="navbar-account" ref={accountRef}>
              <button
                type="button"
                className="navbar-account-btn"
                onClick={() => setAccountOpen((open) => !open)}
                aria-expanded={accountOpen}
                aria-haspopup="true"
              >
                <span className="navbar-avatar" aria-hidden="true">
                  {user?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
                </span>
                <span className="navbar-account-name">
                  {user?.fullName?.split(' ')[0]}
                </span>
              </button>

              {accountOpen && (
                <div className="navbar-dropdown" role="menu">
                  <div className="navbar-dropdown-header">
                    <strong>{user?.fullName}</strong>
                    <span className="text-xs text-muted">{user?.email}</span>
                  </div>
                  <Link to="/profile" className="navbar-dropdown-item" role="menuitem">My Profile</Link>
                  <Link to="/orders" className="navbar-dropdown-item" role="menuitem">My Orders</Link>
                  <Link to="/addresses" className="navbar-dropdown-item" role="menuitem">Addresses</Link>
                  {isAdmin && (
                    <Link to="/admin" className="navbar-dropdown-item" role="menuitem">
                      Admin Dashboard
                    </Link>
                  )}
                  <button
                    type="button"
                    className="navbar-dropdown-item is-danger"
                    onClick={handleLogout}
                    role="menuitem"
                  >
                    Sign out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="navbar-auth">
              <Link to="/login" className="btn btn-ghost btn-sm">Sign in</Link>
              <Link to="/register" className="btn btn-primary btn-sm navbar-register">Sign up</Link>
            </div>
          )}

          <Link to="/cart" className="navbar-cart" aria-label={`Cart, ${itemCount} items`}>
            <CartIcon size={20} />
            {itemCount > 0 && <span className="navbar-cart-badge">{itemCount > 99 ? '99+' : itemCount}</span>}
          </Link>
        </div>
      </div>
    </header>
  )
}
