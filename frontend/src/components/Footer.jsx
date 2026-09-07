import { Link } from 'react-router-dom'
import './Footer.css'

const CATEGORY_LINKS = [
  { slug: 'books', name: 'Books' },
  { slug: 'grocery', name: 'Grocery' },
  { slug: 'kitchen-utensils', name: 'Kitchen Utensils' },
  { slug: 'clothes', name: 'Clothes' },
  { slug: 'electronics', name: 'Electronics' },
  { slug: 'furniture', name: 'Furniture' },
]

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-brand">
          <div className="footer-logo">
            <span className="footer-logo-mark" aria-hidden="true">रोज़</span>
            <span>Roz Bazaar</span>
          </div>
          <p className="footer-tagline">
            Books, groceries, kitchenware, clothing, electronics and furniture — your everyday marketplace.
          </p>
        </div>

        <nav className="footer-column" aria-label="Shop by category">
          <h4 className="footer-heading">Shop</h4>
          {CATEGORY_LINKS.map((category) => (
            <Link key={category.slug} to={`/products?category=${category.slug}`} className="footer-link">
              {category.name}
            </Link>
          ))}
        </nav>

        <nav className="footer-column" aria-label="Your account">
          <h4 className="footer-heading">Account</h4>
          <Link to="/profile" className="footer-link">My Profile</Link>
          <Link to="/orders" className="footer-link">My Orders</Link>
          <Link to="/addresses" className="footer-link">Addresses</Link>
          <Link to="/cart" className="footer-link">Shopping Cart</Link>
        </nav>

        <div className="footer-column">
          <h4 className="footer-heading">About this project</h4>
          <p className="footer-note">
            A full-stack demonstration application built with React, Spring Boot and MySQL,
            with JWT authentication and WhatsApp order notifications.
          </p>
        </div>
      </div>

      <div className="footer-bottom">
        <div className="container">
          <p>© {new Date().getFullYear()} Roz Bazaar. Built as a full-stack learning project.</p>
        </div>
      </div>
    </footer>
  )
}
