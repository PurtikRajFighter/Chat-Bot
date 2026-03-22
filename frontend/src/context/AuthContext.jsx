import React, { createContext, useContext, useState, useEffect } from 'react'
import api from '../api/axios'

/**
 * AuthContext — global auth state.
 * Provides: user, token, login(), logout(), updateTokens()
 * Persists to localStorage so refreshing doesn't log you out.
 */
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    // Load from localStorage on first render
    try {
      const saved = localStorage.getItem('user')
      return saved ? JSON.parse(saved) : null
    } catch { return null }
  })

  const [token, setToken] = useState(() => localStorage.getItem('token') || null)

  // Keep localStorage in sync whenever user/token changes
  useEffect(() => {
    if (user) localStorage.setItem('user', JSON.stringify(user))
    else localStorage.removeItem('user')
  }, [user])

  useEffect(() => {
    if (token) localStorage.setItem('token', token)
    else localStorage.removeItem('token')
  }, [token])

  const login = (userData, jwt) => {
    setUser(userData)
    setToken(jwt)
  }

  const logout = () => {
    setUser(null)
    setToken(null)
  }

  // Update token balance after messages or payments
  const updateTokens = (newBalance) => {
    setUser(prev => prev ? { ...prev, tokens: newBalance } : prev)
  }

  // Refresh user info from server
  const refreshUser = async () => {
    try {
      const res = await api.get('/auth/me')
      if (res.data.success) {
        setUser(prev => ({ ...prev, ...res.data }))
      }
    } catch (err) {
      console.error('Failed to refresh user', err)
    }
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, updateTokens, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)

