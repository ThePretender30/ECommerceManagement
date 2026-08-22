import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Breadcrumbs,
  ErrorState,
  Loader,
  Pagination,
  QuantityStepper,
  StarRating,
} from '../components/Common'
import { ConfirmDialog } from '../components/Modal'
import productService from '../services/productService'
import { useAuth, useCart, useToast } from '../hooks'
import { formatCurrency, formatDate, handleImageError, FALLBACK_IMAGE } from '../utils/format'
import './ProductDetail.css'

export default function ProductDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const { addItem } = useCart()
  const toast = useToast()

  const [product, setProduct] = useState(null)
  const [summary, setSummary] = useState(null)
  const [reviews, setReviews] = useState(null)
  const [reviewPage, setReviewPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [quantity, setQuantity] = useState(1)
  const [adding, setAdding] = useState(false)

  const [reviewForm, setReviewForm] = useState({ rating: 5, reviewText: '' })
  const [submittingReview, setSubmittingReview] = useState(false)
  const [showReviewForm, setShowReviewForm] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const loadProduct = useCallback(() => {
    setLoading(true)
    setError(null)

    Promise.all([productService.getById(id), productService.reviewSummary(id)])
      .then(([productData, summaryData]) => {
        setProduct(productData)
        setSummary(summaryData)
        if (summaryData.userReview) {
          setReviewForm({
            rating: summaryData.userReview.rating,
            reviewText: summaryData.userReview.reviewText ?? '',
          })
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    loadProduct()
    setQuantity(1)
    setReviewPage(0)
    window.scrollTo({ top: 0 })
  }, [loadProduct])

  useEffect(() => {
    productService
      .reviews(id, reviewPage, 5)
      .then(setReviews)
      .catch(() => setReviews(null))
  }, [id, reviewPage])

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      toast.info('Please sign in to add items to your cart.')
      navigate(`/login?redirect=${encodeURIComponent(`/products/${id}`)}`)
      return
    }

    setAdding(true)
    try {
      await addItem(product.id, quantity)
      toast.success(`${quantity} × ${product.name} added to your cart.`)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setAdding(false)
    }
  }

  const handleBuyNow = async () => {
    if (!isAuthenticated) {
      navigate(`/login?redirect=${encodeURIComponent(`/products/${id}`)}`)
      return
    }
    try {
      await addItem(product.id, quantity)
      navigate('/checkout')
    } catch (err) {
      toast.error(err.message)
    }
  }

  const handleSubmitReview = async (event) => {
    event.preventDefault()
    setSubmittingReview(true)
    try {
      await productService.submitReview(id, reviewForm)
      toast.success('Thanks for your review!')
      setShowReviewForm(false)
      loadProduct()
      setReviewPage(0)
      const refreshed = await productService.reviews(id, 0, 5)
      setReviews(refreshed)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSubmittingReview(false)
    }
  }

  const handleDeleteReview = async () => {
    try {
      await productService.deleteOwnReview(id)
      toast.success('Your review has been removed.')
      setConfirmDelete(false)
      setReviewForm({ rating: 5, reviewText: '' })
      loadProduct()
      const refreshed = await productService.reviews(id, 0, 5)
      setReviews(refreshed)
      setReviewPage(0)
    } catch (err) {
      toast.error(err.message)
      setConfirmDelete(false)
    }
  }

  if (loading) return <Loader fullPage label="Loading product…" />
  if (error) return <ErrorState message={error} onRetry={loadProduct} />
  if (!product) return null

  const maxQuantity = Math.min(product.stock ?? 1, 99)

  return (
    <div className="page container">
      <Breadcrumbs
        items={[
          { label: 'Home', to: '/' },
          { label: 'Products', to: '/products' },
          ...(product.categorySlug
            ? [{ label: product.categoryName, to: `/products?category=${product.categorySlug}` }]
            : []),
          { label: product.name },
        ]}
      />

      <div className="detail">
        <div className="detail-media">
          <img
            src={product.imageUrl || FALLBACK_IMAGE}
            alt={product.name}
            onError={handleImageError}
          />
        </div>

        <div className="detail-info">
          {product.categoryName && (
            <span className="detail-category">{product.categoryName}</span>
          )}
          <h1 className="detail-name">{product.name}</h1>
          {product.brand && <p className="detail-brand">by {product.brand}</p>}

          <div className="detail-rating">
            {product.reviewCount > 0 ? (
              <StarRating value={product.averageRating} count={product.reviewCount} size="lg" />
            ) : (
              <span className="text-sm text-subtle">No reviews yet — be the first.</span>
            )}
          </div>

          <p className="detail-price">{formatCurrency(product.price)}</p>

          <div className="detail-stock">
            {product.inStock ? (
              product.stock <= 5 ? (
                <span className="badge badge-warning">Only {product.stock} left in stock</span>
              ) : (
                <span className="badge badge-success">In stock ({product.stock} available)</span>
              )
            ) : (
              <span className="badge badge-danger">Out of stock</span>
            )}
          </div>

          {product.description && (
            <div className="detail-description">
              <h2 className="detail-section-heading">About this product</h2>
              <p>{product.description}</p>
            </div>
          )}

          {product.inStock && (
            <div className="detail-purchase">
              <div className="detail-qty">
                <span className="form-label">Quantity</span>
                <QuantityStepper
                  value={quantity}
                  onChange={setQuantity}
                  min={1}
                  max={maxQuantity}
                  disabled={adding}
                />
              </div>

              <div className="detail-actions">
                <button
                  type="button"
                  className="btn btn-outline btn-lg"
                  onClick={handleAddToCart}
                  disabled={adding}
                >
                  {adding ? 'Adding…' : 'Add to cart'}
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-lg"
                  onClick={handleBuyNow}
                  disabled={adding}
                >
                  Buy now
                </button>
              </div>
            </div>
          )}

          <dl className="detail-meta">
            <div>
              <dt>Product ID</dt>
              <dd>#{product.id}</dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{product.categoryName ?? '—'}</dd>
            </div>
            <div>
              <dt>Brand</dt>
              <dd>{product.brand ?? '—'}</dd>
            </div>
            <div>
              <dt>Listed on</dt>
              <dd>{formatDate(product.dateAdded)}</dd>
            </div>
          </dl>
        </div>
      </div>

      <section className="reviews">
        <h2 className="reviews-title">Ratings and reviews</h2>

        <div className="reviews-layout">
          <div className="reviews-summary">
            <div className="reviews-score">
              <span className="reviews-average">
                {Number(summary?.averageRating ?? 0).toFixed(1)}
              </span>
              <StarRating value={summary?.averageRating ?? 0} showValue={false} size="md" />
              <span className="reviews-total">
                {summary?.totalReviews ?? 0} {summary?.totalReviews === 1 ? 'review' : 'reviews'}
              </span>
            </div>

            <div className="reviews-breakdown">
              {[5, 4, 3, 2, 1].map((stars) => {
                const count = summary?.ratingBreakdown?.[stars] ?? 0
                const total = summary?.totalReviews || 1
                const percent = Math.round((count / total) * 100)
                return (
                  <div key={stars} className="reviews-bar-row">
                    <span className="reviews-bar-label">{stars} ★</span>
                    <span className="reviews-bar-track">
                      <span className="reviews-bar-fill" style={{ width: `${percent}%` }} />
                    </span>
                    <span className="reviews-bar-count">{count}</span>
                  </div>
                )
              })}
            </div>

            {summary?.canReview && !showReviewForm && (
              <button
                type="button"
                className="btn btn-primary btn-block mt-4"
                onClick={() => setShowReviewForm(true)}
              >
                Write a review
              </button>
            )}

            {summary?.userReview && !showReviewForm && (
              <div className="reviews-own">
                <p className="text-xs text-muted mb-2">Your review</p>
                <StarRating value={summary.userReview.rating} showValue={false} size="sm" />
                {summary.userReview.reviewText && (
                  <p className="text-sm mt-2">{summary.userReview.reviewText}</p>
                )}
                <div className="row mt-4">
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    onClick={() => setShowReviewForm(true)}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => setConfirmDelete(true)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            )}

            {!isAuthenticated && (
              <p className="text-xs text-muted mt-4">
                Sign in to review products you have received.
              </p>
            )}

            {isAuthenticated && !summary?.canReview && !summary?.userReview && (
              <p className="text-xs text-muted mt-4">
                You can review this product once an order containing it has been delivered.
              </p>
            )}
          </div>

          <div className="reviews-list">
            {showReviewForm && (
              <form className="review-form card" onSubmit={handleSubmitReview}>
                <div className="card-body">
                  <h3 className="text-lg mb-4">
                    {summary?.userReview ? 'Edit your review' : 'Write a review'}
                  </h3>

                  <div className="form-group">
                    <span className="form-label">Your rating</span>
                    <div className="review-stars">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <button
                          key={star}
                          type="button"
                          className={star <= reviewForm.rating ? 'review-star is-on' : 'review-star'}
                          onClick={() => setReviewForm((f) => ({ ...f, rating: star }))}
                          aria-label={`${star} star${star > 1 ? 's' : ''}`}
                        >
                          ★
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label" htmlFor="reviewText">Your review (optional)</label>
                    <textarea
                      id="reviewText"
                      className="form-control"
                      rows="4"
                      maxLength={2000}
                      placeholder="What did you like or dislike about this product?"
                      value={reviewForm.reviewText}
                      onChange={(e) => setReviewForm((f) => ({ ...f, reviewText: e.target.value }))}
                    />
                  </div>

                  <div className="row wrap">
                    <button type="submit" className="btn btn-primary" disabled={submittingReview}>
                      {submittingReview ? 'Submitting…' : 'Submit review'}
                    </button>
                    <button
                      type="button"
                      className="btn btn-ghost"
                      onClick={() => setShowReviewForm(false)}
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </form>
            )}

            {reviews?.empty !== false ? (
              <p className="text-sm text-muted">
                No reviews yet. Reviews appear here once verified customers write them.
              </p>
            ) : (
              <>
                {reviews.content.map((review) => (
                  <article key={review.id} className="review-item">
                    <div className="review-item-header">
                      <span className="review-avatar" aria-hidden="true">
                        {review.userName?.charAt(0)?.toUpperCase() ?? '?'}
                      </span>
                      <div>
                        <p className="review-author">{review.userName}</p>
                        <div className="row">
                          <StarRating value={review.rating} showValue={false} size="sm" />
                          <span className="text-xs text-subtle">{formatDate(review.createdAt)}</span>
                        </div>
                      </div>
                    </div>
                    {review.reviewText && <p className="review-text">{review.reviewText}</p>}
                  </article>
                ))}

                <Pagination
                  page={reviews.page}
                  totalPages={reviews.totalPages}
                  onPageChange={setReviewPage}
                />
              </>
            )}
          </div>
        </div>
      </section>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete your review?"
        message="This will permanently remove your rating and comment for this product."
        confirmLabel="Delete review"
        danger
        onConfirm={handleDeleteReview}
        onCancel={() => setConfirmDelete(false)}
      />
    </div>
  )
}
