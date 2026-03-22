import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import './Auth.css'

/**
 * AuthPage — Login and Register combined on one page.
 * Toggle between login/register with a tab.
 */
export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true)
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const endpoint = isLogin ? '/auth/login' : '/auth/register'
      const payload = isLogin
        ? { username: form.username, password: form.password }
        : { username: form.username, email: form.email, password: form.password }

      const res = await api.post(endpoint, payload)

      if (res.data.success) {
        // Store user info and JWT in context
        login(
          { userId: res.data.userId, username: res.data.username, tokens: res.data.tokens },
          res.data.token
        )
        navigate('/chat')
      } else {
        setError(res.data.error || 'Something went wrong')
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Server error. Make sure the backend is running.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Header */}
        <div className="auth-header">
          <div className="auth-logo">🤖</div>
          <h1 className="auth-title">AI ChatBot</h1>
          <p className="auth-subtitle">Your local AI companion</p>
        </div>

        {/* Tab toggle */}
        <div className="auth-tabs">
          <button
            className={`auth-tab ${isLogin ? 'active' : ''}`}
            onClick={() => { setIsLogin(true); setError('') }}
          >
            Login
          </button>
          <button
            className={`auth-tab ${!isLogin ? 'active' : ''}`}
            onClick={() => { setIsLogin(false); setError('') }}
          >
            Register
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label>Username</label>
            <input
              className="input"
              type="text"
              name="username"
              placeholder="Enter username"
              value={form.username}
              onChange={handleChange}
              required
              autoFocus
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label>Email</label>
              <input
                className="input"
                type="email"
                name="email"
                placeholder="Enter email"
                value={form.email}
                onChange={handleChange}
                required={!isLogin}
              />
            </div>
          )}

          <div className="form-group">
            <label>Password</label>
            <input
              className="input"
              type="password"
              name="password"
              placeholder={isLogin ? 'Enter password' : 'Min 6 characters'}
              value={form.password}
              onChange={handleChange}
              required
            />
          </div>

          {error && <div className="alert alert-error">{error}</div>}

          <button type="submit" className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Please wait...' : (isLogin ? 'Login' : 'Create Account')}
          </button>
        </form>

        {!isLogin && (
          <p className="auth-note">
            🎁 You'll get <strong>10 free tokens</strong> on registration!
          </p>
        )}
      </div>
    </div>
  )
}

