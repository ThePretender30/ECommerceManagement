import api from './api'

/**
 * Product browsing, search and reviews.
 *
 * `list` mirrors the backend's single filter endpoint. Undefined/empty values
 * are stripped so the URL only carries filters that are actually applied - which
 * keeps shareable URLs clean and avoids sending `brand=` as an empty filter.
 */
export const productService = {
  list: (params = {}) => {
    const query = {}
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query[key] = value
      }
    })
    return api.get('/products', { params: query }).then((r) => r.data)
  },

  getById: (id) => api.get(`/products/${id}`).then((r) => r.data),

  featured: () => api.get('/products/featured').then((r) => r.data),
  popular: () => api.get('/products/popular').then((r) => r.data),
  newArrivals: () => api.get('/products/new-arrivals').then((r) => r.data),

  brands: (categoryId) =>
    api.get('/products/brands', { params: categoryId ? { categoryId } : {} }).then((r) => r.data),

  // ---- Reviews ----
  reviews: (productId, page = 0, size = 10) =>
    api.get(`/products/${productId}/reviews`, { params: { page, size } }).then((r) => r.data),

  reviewSummary: (productId) =>
    api.get(`/products/${productId}/reviews/summary`).then((r) => r.data),

  submitReview: (productId, payload) =>
    api.post(`/products/${productId}/reviews`, payload).then((r) => r.data),

  deleteOwnReview: (productId) =>
    api.delete(`/products/${productId}/reviews/mine`).then((r) => r.data),
}

export default productService
