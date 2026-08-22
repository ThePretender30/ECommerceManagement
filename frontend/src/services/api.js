import axios from 'axios'

const TOKEN_KEY = 'ecommerce.token'
const USER_KEY = 'ecommerce.user'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
})

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },
}

export const userStorage = {
  get: () => {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  },
  set: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
}

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let onUnauthorized = null
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      const networkError = new Error(
        error.code === 'ECONNABORTED'
          ? 'The server took too long to respond. Please try again.'
          : 'Cannot reach the server. Is the backend running on port 8080?'
      )
      networkError.isNetworkError = true
      return Promise.reject(networkError)
    }

    const { status, data } = error.response

    if (status === 401) {
      const url = error.config?.url ?? ''
      const isLoginAttempt = url.includes('/auth/login') || url.includes('/auth/register')
      if (!isLoginAttempt) {
        tokenStorage.clear()
        if (onUnauthorized) onUnauthorized()
      }
    }

    const apiError = new Error(data?.message || defaultMessageFor(status))
    apiError.status = status
    apiError.fieldErrors = data?.fieldErrors || null
    return Promise.reject(apiError)
  }
)

function defaultMessageFor(status) {
  switch (status) {
    case 400: return 'That request was not valid.'
    case 403: return 'You do not have permission to do that.'
    case 404: return 'We could not find what you were looking for.'
    case 409: return 'That conflicts with existing data.'
    case 500: return 'Something went wrong on the server. Please try again.'
    default:  return 'Something went wrong. Please try again.'
  }
}

export default api
