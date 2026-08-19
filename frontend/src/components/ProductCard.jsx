import { Link, useNavigate } from 'react-router-dom'
import { StarRating } from './Common'
import { useAuth, useCart, useToast } from '../hooks'
import { formatCurrency, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './ProductCard.css'

/**
 * A single product tile, used by every grid in the app.
 *
 * The add-to-cart button lives here so the action is available from listings
 * without a detour through the detail page. A signed-out visitor is sent to
 * login with a `redirect` back to where they were, so the click is not lost.
 */
export default function ProductCard({ product }) {
  const { isAuthenticated } = useAuth()
  const { addItem } = useCart()
  const toast = useToast()
  const navigate = useNavigate()

  const outOfStock = !product.inStock
  const lowStock = product.inStock && product.stock <= 5

  const handleAddToCart = async (event) => {
    // The whole card is a link; stop the click from navigating as well.
    event.preventDefault()
    event.stopPropagation()

    if (!isAuthenticated) {
      toast.info('Please sign in to add items to your cart.')
      navigate(`/login?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`)
      return
    }

    try {
      await addItem(product.id, 1)
      toast.success(`${product.name} added to your cart.`)
    } catch (error) {
      toast.error(error.message)
    }
  }

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
          {lowStock && <span className="product-card-flag is-low">Only {product.stock} left</span>}
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
          </div>
        </div>
      </Link>

      <button
        type="button"
        className="btn btn-primary btn-sm product-card-add"
        onClick={handleAddToCart}
        disabled={outOfStock}
      >
        {outOfStock ? 'Unavailable' : 'Add to cart'}
      </button>
    </article>
  )
}
