import api from './api'

export const authService = {
  initiateRegister: (payload) => api.post('/auth/register', payload).then((r) => r.data),

  verifyRegisterOtp: (sessionToken, otp) =>
    api.post('/auth/register/verify', { sessionToken, otp }).then((r) => r.data),

  resendRegisterOtp: (sessionToken) =>
    api.post('/auth/register/resend', { sessionToken }).then((r) => r.data),

  login: (email, password) =>
    api.post('/auth/login', { email, password }).then((r) => r.data),

  loginAdmin: (email, password) =>
    api.post('/auth/admin/login', { email, password }).then((r) => r.data),

  me: () => api.get('/auth/me').then((r) => r.data),

  updateProfile: (payload) => api.put('/auth/me', payload).then((r) => r.data),

  changePassword: (currentPassword, newPassword) =>
    api.put('/auth/me/password', { currentPassword, newPassword }).then((r) => r.data),

  logout: () => api.post('/auth/logout').then((r) => r.data),
}

export default authService
