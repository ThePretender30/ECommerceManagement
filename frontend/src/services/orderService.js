import api from './api'

export const orderService = {
  place: (payload) => api.post('/orders', payload).then((r) => r.data),

  list: (page = 0, size = 10) =>
    api.get('/orders', { params: { page, size } }).then((r) => r.data),

  getById: (id) => api.get(`/orders/${id}`).then((r) => r.data),

  track: (id) => api.get(`/orders/${id}/tracking`).then((r) => r.data),

  cancel: (id, reason) =>
    api.put(`/orders/${id}/cancel`, null, { params: reason ? { reason } : {} }).then((r) => r.data),
}

export default orderService
