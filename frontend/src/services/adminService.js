import api from './api'

/**
 * Admin-only calls. Every path is under /api/admin, which the backend gates
 * behind ROLE_ADMIN - the AdminRoute guard in the UI is convenience, not the
 * actual protection.
 */
export const adminService = {
  // ---- Dashboard ----
  stats: () => api.get('/admin/stats').then((r) => r.data),

  // ---- Products ----
  listProducts: (params = {}) =>
    api.get('/admin/products', { params }).then((r) => r.data),
  getProduct: (id) => api.get(`/admin/products/${id}`).then((r) => r.data),
  createProduct: (payload) => api.post('/admin/products', payload).then((r) => r.data),
  updateProduct: (id, payload) => api.put(`/admin/products/${id}`, payload).then((r) => r.data),
  updateStock: (id, stock) =>
    api.patch(`/admin/products/${id}/stock`, { stock }).then((r) => r.data),
  deleteProduct: (id) => api.delete(`/admin/products/${id}`).then((r) => r.data),

  // ---- Categories ----
  listCategories: () => api.get('/admin/categories').then((r) => r.data),
  createCategory: (payload) => api.post('/admin/categories', payload).then((r) => r.data),
  updateCategory: (id, payload) => api.put(`/admin/categories/${id}`, payload).then((r) => r.data),
  deleteCategory: (id) => api.delete(`/admin/categories/${id}`).then((r) => r.data),

  // ---- Orders ----
  listOrders: (params = {}) => api.get('/admin/orders', { params }).then((r) => r.data),
  getOrder: (id) => api.get(`/admin/orders/${id}`).then((r) => r.data),
  updateOrderStatus: (id, status, note) =>
    api.put(`/admin/orders/${id}/status`, { status, note }).then((r) => r.data),
  /** Status list with the legal transitions from each, used to build the UI. */
  orderStatuses: () => api.get('/admin/orders/statuses').then((r) => r.data),
  orderNotifications: (id) => api.get(`/admin/orders/${id}/notifications`).then((r) => r.data),

  // ---- Users ----
  listUsers: (params = {}) => api.get('/admin/users', { params }).then((r) => r.data),
  getUser: (id) => api.get(`/admin/users/${id}`).then((r) => r.data),
  setUserEnabled: (id, enabled) =>
    api.put(`/admin/users/${id}/enabled`, null, { params: { enabled } }).then((r) => r.data),

  // ---- Notifications ----
  listNotifications: (params = {}) =>
    api.get('/admin/notifications', { params }).then((r) => r.data),
  notificationSummary: () => api.get('/admin/notifications/summary').then((r) => r.data),
}

export default adminService
