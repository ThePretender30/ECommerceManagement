import api from './api'

export const couponService = {
  validate: (code, subtotal) =>
    api.post('/coupons/validate', { code, subtotal }).then((r) => r.data),

  // Admin APIs
  listAll: () => api.get('/admin/coupons').then((r) => r.data),

  create: (payload) => api.post('/admin/coupons', payload).then((r) => r.data),

  toggle: (id) => api.patch(`/admin/coupons/${id}/toggle`).then((r) => r.data),

  delete: (id) => api.delete(`/admin/coupons/${id}`).then((r) => r.data),
}

export default couponService
