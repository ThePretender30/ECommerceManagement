import api from './api'

export const categoryService = {
  list: () => api.get('/categories').then((r) => r.data),
  getBySlug: (slug) => api.get(`/categories/slug/${slug}`).then((r) => r.data),
  getById: (id) => api.get(`/categories/${id}`).then((r) => r.data),
}

export const cartService = {
  get: () => api.get('/cart').then((r) => r.data),

  addItem: (productId, quantity = 1) =>
    api.post('/cart/items', { productId, quantity }).then((r) => r.data),

  updateItem: (itemId, quantity) =>
    api.put(`/cart/items/${itemId}`, { quantity }).then((r) => r.data),

  removeItem: (itemId) => api.delete(`/cart/items/${itemId}`).then((r) => r.data),

  clear: () => api.delete('/cart').then((r) => r.data),
}

export const addressService = {
  list: () => api.get('/addresses').then((r) => r.data),
  getById: (id) => api.get(`/addresses/${id}`).then((r) => r.data),
  create: (payload) => api.post('/addresses', payload).then((r) => r.data),
  update: (id, payload) => api.put(`/addresses/${id}`, payload).then((r) => r.data),
  setDefault: (id) => api.put(`/addresses/${id}/default`).then((r) => r.data),
  remove: (id) => api.delete(`/addresses/${id}`).then((r) => r.data),
}
