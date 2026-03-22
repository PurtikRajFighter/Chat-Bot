import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import AuthPage from './pages/AuthPage'
import ChatPage from './pages/ChatPage'

/**
 * ProtectedRoute — redirects to /login if user is not authenticated.
 */
function ProtectedRoute({ children }) {
  const { user, token } = useAuth()
  if (!user || !token) {
    return <Navigate to="/login" replace />
  }
  return children
}

/**
 * PublicRoute — redirects to /chat if already logged in.
 */
function PublicRoute({ children }) {
  const { user, token } = useAuth()
  if (user && token) {
    return <Navigate to="/chat" replace />
  }
  return children
}

/**
 * App — root component with routing.
 * Routes:
 *   /        → redirect to /chat or /login
 *   /login   → AuthPage (public only)
 *   /chat    → ChatPage (protected)
 */
function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/chat" replace />} />
      <Route
        path="/login"
        element={
          <PublicRoute>
            <AuthPage />
          </PublicRoute>
        }
      />
      <Route
        path="/chat"
        element={
          <ProtectedRoute>
            <ChatPage />
          </ProtectedRoute>
        }
      />
      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

