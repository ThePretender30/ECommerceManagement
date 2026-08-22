import api from './api'

export const authService = {
  register: (payload) => api.post('/auth/register', payload).then((r) => r.data),

  login: (email, password) =>
    api.post('/auth/login', { email, password }).then((r) => r.data),

  me: () => api.get('/auth/me').then((r) => r.data),

  updateProfile: (payload) => api.put('/auth/me', payload).then((r) => r.data),

  changePassword: (currentPassword, newPassword) =>
    api.put('/auth/me/password', { currentPassword, newPassword }).then((r) => r.data),

  logout: () => api.post('/auth/logout').then((r) => r.data),
}

export default authService
