import axios from 'axios'

/**
 * Axios instance pre-configured for our backend.
 * - Base URL points to Spring Boot (proxied via Vite in dev)
 * - Automatically attaches JWT token from localStorage to every request
 * - Handles 401 (token expired) by clearing storage and redirecting to login
 */
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor — add JWT token to Authorization header
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor — handle 401 (expired/invalid token)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api

