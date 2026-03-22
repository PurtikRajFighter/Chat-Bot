import React, { useEffect, useRef, useState } from 'react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import './ChatWindow.css'

/**
 * ChatWindow — the main chat interface.
 * Shows message history, handles sending messages, displays typing indicator.
 */
export default function ChatWindow({ mode }) {
  const { user, updateTokens } = useAuth()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [loadingHistory, setLoadingHistory] = useState(true)
  const messagesEndRef = useRef(null)

  // Load chat history whenever mode changes
  useEffect(() => {
    loadHistory()
  }, [mode])

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const loadHistory = async () => {
    setLoadingHistory(true)
    try {
      const res = await api.get(`/chat/history?mode=${mode}`)
      if (res.data.success) {
        setMessages(res.data.chats || [])
      }
    } catch (err) {
      console.error('Failed to load history', err)
    } finally {
      setLoadingHistory(false)
    }
  }

  const sendMessage = async () => {
    const text = input.trim()
    if (!text || loading) return

    setInput('')
    setError('')

    // Optimistically add user message to UI
    const tempUserMsg = {
      id: 'temp-' + Date.now(),
      role: 'user',
      message: text,
      mode,
      timestamp: new Date().toISOString(),
    }
    setMessages(prev => [...prev, tempUserMsg])
    setLoading(true)

    try {
      const res = await api.post('/chat/send', { message: text, mode })

      if (res.data.success) {
        // Update token balance
        updateTokens(res.data.tokens)

        // Add AI reply to messages
        const aiMsg = {
          id: 'ai-' + Date.now(),
          role: 'assistant',
          message: res.data.reply,
          mode,
          timestamp: new Date().toISOString(),
        }
        setMessages(prev => [...prev, aiMsg])
      } else {
        setError(res.data.error || 'Failed to get response')
        // Remove the optimistic message on error
        setMessages(prev => prev.filter(m => m.id !== tempUserMsg.id))
      }
    } catch (err) {
      const errMsg = err.response?.data?.error || 'Server error'
      setError(errMsg)
      setMessages(prev => prev.filter(m => m.id !== tempUserMsg.id))
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  const getModeColor = () => {
    const colors = { NORMAL: '#667eea', FANTASY: '#ed8936', COMPANION: '#48bb78', FLIRT: '#ed64a6' }
    return colors[mode] || '#667eea'
  }

  const getModeWelcome = () => {
    const msgs = {
      NORMAL: "Hello! I'm your helpful AI assistant. Ask me anything!",
      FANTASY: "Greetings, brave adventurer! ⚔️ What story shall we weave today?",
      COMPANION: "Hey there! 😊 I'm here to chat and listen. How are you doing?",
      FLIRT: "Well, hello there 💫 You look like someone with great taste in chatbots. What's on your mind?",
    }
    return msgs[mode] || msgs.NORMAL
  }

  return (
    <div className="chat-window">
      {/* Messages area */}
      <div className="messages-area">
        {loadingHistory ? (
          <div className="loading-history">Loading chat history...</div>
        ) : (
          <>
            {/* Welcome message if no history */}
            {messages.length === 0 && (
              <div className="welcome-msg" style={{ borderColor: getModeColor() }}>
                <div className="welcome-text">{getModeWelcome()}</div>
              </div>
            )}

            {/* Chat messages */}
            {messages.map((msg) => (
              <MessageBubble key={msg.id} msg={msg} modeColor={getModeColor()} />
            ))}

            {/* Typing indicator while waiting for AI */}
            {loading && (
              <div className="message-row assistant">
                <div className="bubble assistant-bubble" style={{ borderColor: getModeColor() }}>
                  <TypingIndicator />
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Error message */}
      {error && (
        <div className="chat-error">
          <span>{error}</span>
          {error.includes('tokens') && (
            <span className="error-hint"> → Use the "Buy Tokens" button above ↑</span>
          )}
        </div>
      )}

      {/* Input area */}
      <div className="input-area">
        <textarea
          className="chat-input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={`Message in ${mode} mode... (Enter to send, Shift+Enter for new line)`}
          rows={1}
          disabled={loading || user?.tokens === 0}
        />
        <button
          className="send-btn"
          onClick={sendMessage}
          disabled={loading || !input.trim() || user?.tokens === 0}
          style={{ background: getModeColor() }}
        >
          {loading ? '⏳' : '➤'}
        </button>
      </div>

      {user?.tokens === 0 && (
        <div className="no-tokens-warning">
          ⚠️ You're out of tokens! Buy more above to continue chatting.
        </div>
      )}
    </div>
  )
}

function MessageBubble({ msg, modeColor }) {
  const isUser = msg.role === 'user'
  const time = msg.timestamp
    ? new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : ''

  return (
    <div className={`message-row ${isUser ? 'user' : 'assistant'}`}>
      {!isUser && <div className="avatar ai-avatar">🤖</div>}
      <div className={`bubble ${isUser ? 'user-bubble' : 'assistant-bubble'}`}
           style={!isUser ? { borderColor: modeColor } : {}}>
        <div className="bubble-text">{msg.message}</div>
        <div className="bubble-time">{time}</div>
      </div>
      {isUser && <div className="avatar user-avatar">👤</div>}
    </div>
  )
}

function TypingIndicator() {
  return (
    <div className="typing-indicator">
      <span></span><span></span><span></span>
    </div>
  )
}

