import axios from 'axios'

/**
 * The single axios instance every service in this app builds on.
 *
 * Two interceptors carry almost all of the cross-cutting behaviour:
 *
 *  - Request:  attaches the JWT, so no individual call has to remember to.
 *  - Response: normalises the backend's ApiError shape into a plain Error with
 *              a readable `.message` and an optional `.fieldErrors`, and signs
 *              the user out on a 401.
 *
 * Because the backend always returns the same error shape, one place can turn
 * every failure into something a component can render.
 */

const TOKEN_KEY = 'ecommerce.token'
const USER_KEY = 'ecommerce.user'

// Relative baseURL: Vite proxies /api to :8080 in dev, and in production the
// frontend is served from the same origin as the API.
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
})

/* ---------------------------------------------------------------- token store */

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
      // Corrupted entry - treat it as signed out rather than crashing on boot.
      return null
    }
  },
  set: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
}

/* -------------------------------------------------------------- interceptors */

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Lets AuthContext register what should happen when the server rejects our
 * token. Keeping it as a callback avoids importing React state into this
 * module, and avoids a hard `window.location` redirect that would throw away
 * the SPA's router state.
 */
let onUnauthorized = null
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // No response at all - the backend is down or unreachable.
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

    // 401 means the token is missing, expired or invalid: sign out.
    // 403 means "logged in, but not allowed" and must NOT sign the user out.
    if (status === 401) {
      const url = error.config?.url ?? ''
      // A failed login attempt is an expected 401, not a session expiry.
      const isLoginAttempt = url.includes('/auth/login') || url.includes('/auth/register')
      if (!isLoginAttempt) {
        tokenStorage.clear()
        if (onUnauthorized) onUnauthorized()
      }
    }

    // Unwrap the backend's consistent ApiError body.
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
