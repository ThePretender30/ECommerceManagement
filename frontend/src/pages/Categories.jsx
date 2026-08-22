import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Breadcrumbs, ErrorState, Loader } from '../components/Common'
import { categoryService } from '../services/catalogService'
import { handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './Categories.css'

export default function Categories() {
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)
    categoryService
      .list()
      .then(setCategories)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  if (loading) return <Loader fullPage label="Loading categories…" />
  if (error) return <ErrorState message={error} onRetry={load} />

  return (
    <div className="page container">
      <Breadcrumbs items={[{ label: 'Home', to: '/' }, { label: 'Categories' }]} />

      <div className="page-header">
        <h1 className="page-title">Shop by category</h1>
        <p className="page-subtitle">Six departments covering everything the store sells.</p>
      </div>

      <div className="categories-grid">
        {categories.map((category) => (
          <Link
            key={category.id}
            to={`/products?category=${category.slug}`}
            className="category-tile"
          >
            <div className="category-tile-image">
              <img
                src={category.imageUrl || FALLBACK_IMAGE}
                alt={category.name}
                loading="lazy"
                onError={handleImageError}
              />
            </div>
            <div className="category-tile-body">
              <h2 className="category-tile-name">{category.name}</h2>
              {category.description && (
                <p className="category-tile-description">{category.description}</p>
              )}
              <span className="category-tile-count">
                {category.productCount ?? 0} {category.productCount === 1 ? 'product' : 'products'}
              </span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
