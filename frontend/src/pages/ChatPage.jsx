import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ModeSelector from '../components/ModeSelector'
import ChatWindow from '../components/ChatWindow'
import TokenDashboard from '../components/TokenDashboard'
import './ChatPage.css'

/**
 * ChatPage — the main page after login.
 * Layout: sidebar (mode + tokens) + chat window
 */
export default function ChatPage() {
  const [mode, setMode] = useState('NORMAL')
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="chat-page">
      {/* Sidebar */}
      <div className="sidebar">
        {/* App header */}
        <div className="sidebar-header">
          <div className="app-logo">🤖</div>
          <div className="app-info">
            <h2 className="app-name">AI ChatBot</h2>
            <p className="user-name">@{user?.username}</p>
          </div>
        </div>

        {/* Token Dashboard */}
        <TokenDashboard />

        {/* Mode Selector */}
        <ModeSelector currentMode={mode} onModeChange={setMode} />

        {/* Spacer */}
        <div style={{ flex: 1 }} />

        {/* Footer: logout */}
        <div className="sidebar-footer">
          <button className="btn btn-outline logout-btn" onClick={handleLogout}>
            🚪 Logout
          </button>
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="chat-main">
        {/* Chat header */}
        <div className="chat-header">
          <div className="chat-header-info">
            <span className="mode-indicator">
              {mode === 'NORMAL' && '🧠'}
              {mode === 'FANTASY' && '🐉'}
              {mode === 'COMPANION' && '🤗'}
              {mode === 'FLIRT' && '💫'}
              {' '}{mode.charAt(0) + mode.slice(1).toLowerCase()} Mode
            </span>
          </div>
          <div className="token-badge">
            🪙 {user?.tokens ?? 0} tokens
          </div>
        </div>

        {/* Chat Window component */}
        <ChatWindow mode={mode} />
      </div>
    </div>
  )
}

