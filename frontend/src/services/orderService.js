import api from './api'

/** Checkout, order history and tracking for the signed-in customer. */
export const orderService = {
  /**
   * Places an order. The payload carries only an address choice - never line
   * items or prices, which the backend reads from the server-side cart.
   */
  place: (payload) => api.post('/orders', payload).then((r) => r.data),

  list: (page = 0, size = 10) =>
    api.get('/orders', { params: { page, size } }).then((r) => r.data),

  getById: (id) => api.get(`/orders/${id}`).then((r) => r.data),

  track: (id) => api.get(`/orders/${id}/tracking`).then((r) => r.data),

  cancel: (id, reason) =>
    api.put(`/orders/${id}/cancel`, null, { params: reason ? { reason } : {} }).then((r) => r.data),
}

export default orderService
