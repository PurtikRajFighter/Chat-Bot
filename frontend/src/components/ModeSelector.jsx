import React from 'react'
import './ModeSelector.css'

/**
 * ModeSelector — lets the user pick a chat personality.
 * Each mode has a different system prompt on the backend.
 */
const MODES = [
  {
    id: 'NORMAL',
    emoji: '🧠',
    label: 'Normal',
    desc: 'Helpful assistant',
    color: '#667eea',
  },
  {
    id: 'FANTASY',
    emoji: '🐉',
    label: 'Fantasy',
    desc: 'Storytelling & adventures',
    color: '#ed8936',
  },
  {
    id: 'COMPANION',
    emoji: '🤗',
    label: 'Companion',
    desc: 'Friendly emotional chat',
    color: '#48bb78',
  },
  {
    id: 'FLIRT',
    emoji: '💫',
    label: 'Flirt',
    desc: 'Playful & charming',
    color: '#ed64a6',
  },
]

export default function ModeSelector({ currentMode, onModeChange }) {
  return (
    <div className="mode-selector">
      <p className="mode-label">Chat Mode</p>
      <div className="mode-grid">
        {MODES.map((mode) => (
          <button
            key={mode.id}
            className={`mode-btn ${currentMode === mode.id ? 'active' : ''}`}
            style={{ '--mode-color': mode.color }}
            onClick={() => onModeChange(mode.id)}
            title={mode.desc}
          >
            <span className="mode-emoji">{mode.emoji}</span>
            <span className="mode-name">{mode.label}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

