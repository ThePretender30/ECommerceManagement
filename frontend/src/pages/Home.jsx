import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { ErrorState, Loader } from '../components/Common'
import { MessageIcon, ShieldCheckIcon, StarIcon, TruckIcon } from '../components/Icons'
import productService from '../services/productService'
import { categoryService } from '../services/catalogService'
import { handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './Home.css'

export default function Home() {
  const [data, setData] = useState({
    categories: [],
    featured: [],
    popular: [],
    newArrivals: [],
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)

    Promise.all([
      categoryService.list(),
      productService.featured(),
      productService.popular(),
      productService.newArrivals(),
    ])
      .then(([categories, featured, popular, newArrivals]) =>
        setData({ categories, featured, popular, newArrivals })
      )
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  if (loading) return <Loader fullPage label="Loading the storefront…" />
  if (error) return <ErrorState message={error} onRetry={load} />

  return (
    <div className="home">
      <section className="hero">
        <div className="container hero-inner">
          <div className="hero-content">
            <span className="hero-eyebrow">Six departments, one checkout</span>
            <h1 className="hero-title">
              Everything you need,<br />delivered to your door.
            </h1>
            <p className="hero-text">
              Browse books, groceries, kitchenware, clothing, electronics and furniture.
              Track every order in real time and get WhatsApp updates at each step.
            </p>
            <div className="hero-actions">
              <Link to="/products" className="btn btn-accent btn-lg">Start shopping</Link>
              <Link to="/categories" className="btn btn-outline btn-lg hero-btn-ghost">
                Browse categories
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="container">
        <div className="value-props">
          <div className="value-prop">
            <span className="value-prop-icon" aria-hidden="true"><TruckIcon size={24} /></span>
            <div>
              <h3>Live order tracking</h3>
              <p>Follow every order from placed to delivered.</p>
            </div>
          </div>
          <div className="value-prop">
            <span className="value-prop-icon" aria-hidden="true"><MessageIcon size={24} /></span>
            <div>
              <h3>WhatsApp updates</h3>
              <p>A message at every stage, straight to your phone.</p>
            </div>
          </div>
          <div className="value-prop">
            <span className="value-prop-icon" aria-hidden="true"><ShieldCheckIcon size={24} /></span>
            <div>
              <h3>Secure accounts</h3>
              <p>Encrypted passwords and token-based sign-in.</p>
            </div>
          </div>
          <div className="value-prop">
            <span className="value-prop-icon" aria-hidden="true"><StarIcon size={24} /></span>
            <div>
              <h3>Verified reviews</h3>
              <p>Only customers who received an item can review it.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="container home-section">
        <div className="home-section-header">
          <div>
            <h2 className="home-section-title">Shop by category</h2>
            <p className="home-section-subtitle">Six departments, thousands of products.</p>
          </div>
          <Link to="/categories" className="home-section-link">View all →</Link>
        </div>

        <div className="category-grid">
          {data.categories.map((category) => (
            <Link
              key={category.id}
              to={`/products?category=${category.slug}`}
              className="category-card"
            >
              <div className="category-card-image">
                <img
                  src={category.imageUrl || FALLBACK_IMAGE}
                  alt={category.name}
                  loading="lazy"
                  onError={handleImageError}
                />
              </div>
              <div className="category-card-body">
                <h3 className="category-card-name">{category.name}</h3>
                <span className="category-card-count">
                  {category.productCount ?? 0} {category.productCount === 1 ? 'item' : 'items'}
                </span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <ProductRow
        title="Featured products"
        subtitle="Our highest rated items right now."
        products={data.featured}
        link="/products?sort=rating"
      />

      <section className="container">
        <div className="promo-band">
          <div>
            <h2 className="promo-title">New here?</h2>
            <p className="promo-text">
              Create an account to save addresses, track orders and get WhatsApp delivery updates.
            </p>
          </div>
          <Link to="/register" className="btn btn-primary btn-lg">Create an account</Link>
        </div>
      </section>

      <ProductRow
        title="Popular right now"
        subtitle="What other shoppers are reviewing most."
        products={data.popular}
        link="/products?sort=popular"
      />

      <ProductRow
        title="New arrivals"
        subtitle="The latest additions to the store."
        products={data.newArrivals}
        link="/products?sort=newest"
      />
    </div>
  )
}

function ProductRow({ title, subtitle, products, link }) {
  if (!products?.length) return null

  return (
    <section className="container home-section">
      <div className="home-section-header">
        <div>
          <h2 className="home-section-title">{title}</h2>
          <p className="home-section-subtitle">{subtitle}</p>
        </div>
        <Link to={link} className="home-section-link">View all →</Link>
      </div>

      <div className="product-grid">
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </section>
  )
}
