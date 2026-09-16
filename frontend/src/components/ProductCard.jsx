import { Link } from 'react-router-dom'
import { StarRating } from './Common'
import { formatCurrency, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './ProductCard.css'

export default function ProductCard({ product }) {
  const outOfStock = !product.inStock
  const lowStock = product.inStock && product.stock <= 5

  return (
    <article className="product-card">
      <Link to={`/products/${product.id}`} className="product-card-link">
        <div className="product-card-image">
          <img
            src={product.imageUrl || FALLBACK_IMAGE}
            alt={product.name}
            loading="lazy"
            onError={handleImageError}
          />
          {outOfStock && <span className="product-card-flag is-out">Out of stock</span>}
          {lowStock && <span className="product-card-flag is-low">⚡ Only {product.stock} left!</span>}
        </div>

        <div className="product-card-body">
          {product.categoryName && (
            <span className="product-card-category">{product.categoryName}</span>
          )}
          <h3 className="product-card-name">{product.name}</h3>
          {product.brand && <p className="product-card-brand">{product.brand}</p>}

          <div className="product-card-rating">
            {product.reviewCount > 0 ? (
              <StarRating value={product.averageRating} count={product.reviewCount} size="sm" />
            ) : (
              <span className="product-card-noreviews">No reviews yet</span>
            )}
          </div>

          <div className="product-card-footer">
            <span className="product-card-price">{formatCurrency(product.price)}</span>
            {lowStock && <span className="product-card-stock-warning">Low Stock</span>}
          </div>
        </div>
      </Link>
    </article>
  )
}
