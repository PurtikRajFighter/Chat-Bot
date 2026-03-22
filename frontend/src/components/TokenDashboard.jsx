import React, { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import './TokenDashboard.css'

/**
 * TokenDashboard — shows current token balance and handles Razorpay payments.
 *
 * Packages are loaded from GET /api/payment/packages — no hardcoding here.
 * To add/remove/change packages, edit application.properties only.
 */

// No BASE_PACKAGES constant needed — everything comes from the backend.

export default function TokenDashboard({ onTokensUpdated }) {
  const { user, updateTokens } = useAuth()
  const [loading, setLoading]           = useState(false)
  const [message, setMessage]           = useState('')
  const [showPackages, setShowPackages] = useState(false)

  // Full package + discount state from backend
  const [discount, setDiscount] = useState({ enabled: false, label: '', globalBonusPercent: 0 })
  const [packages, setPackages] = useState([])   // populated on mount

  // Fetch packages (with live discount info) once on mount
  useEffect(() => {
    api.get('/payment/packages')
      .then(res => {
        const data = res.data
        setDiscount({
          enabled:            data.discountEnabled  || false,
          label:              data.discountLabel     || '',
          globalBonusPercent: data.globalBonusPercent || 0,
        })
        setPackages(data.packages || [])
      })
      .catch(() => {
        // Non-critical — show empty package list with an error note
        setPackages([])
      })
  }, [])

  const handleBuyTokens = async (pkg) => {
    setLoading(true)
    setMessage('')

    try {
      // Step 1: Create order on backend (discount is applied server-side)
      const orderRes = await api.post('/payment/create-order', { amount: pkg.amount })

      if (!orderRes.data.success) {
        setMessage('❌ ' + (orderRes.data.error || 'Failed to create order'))
        return
      }

      const { orderId, amount, currency, keyId, tokensToAdd } = orderRes.data

      // Step 2: Open Razorpay checkout modal
      const options = {
        key: keyId,
        amount: amount,
        currency: currency,
        name: 'AI ChatBot',
        description: `${tokensToAdd} Tokens`,
        order_id: orderId,
        prefill: { name: user?.username || '' },
        theme: { color: '#667eea' },
        handler: async (paymentResponse) => {
          // Step 3: Verify payment on backend
          try {
            const verifyRes = await api.post('/payment/verify', {
              razorpayOrderId:   paymentResponse.razorpay_order_id,
              razorpayPaymentId: paymentResponse.razorpay_payment_id,
              razorpaySignature: paymentResponse.razorpay_signature,
            })

            if (verifyRes.data.success) {
              updateTokens(verifyRes.data.newBalance)
              setMessage(`✅ ${verifyRes.data.tokensAdded} tokens added! New balance: ${verifyRes.data.newBalance}`)
              setShowPackages(false)
              if (onTokensUpdated) onTokensUpdated(verifyRes.data.newBalance)
            } else {
              setMessage('❌ Payment verification failed: ' + verifyRes.data.error)
            }
          } catch (err) {
            setMessage('❌ Verification error: ' + (err.response?.data?.error || err.message))
          }
        },
        modal: {
          ondismiss: () => {
            setMessage('Payment cancelled.')
            setLoading(false)
          }
        }
      }

      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', (resp) => {
        setMessage('❌ Payment failed: ' + resp.error.description)
        setLoading(false)
      })
      rzp.open()

    } catch (err) {
      setMessage('❌ ' + (err.response?.data?.error || 'Something went wrong'))
    } finally {
      setLoading(false)
    }
  }

  // Token bar color: green > 50%, yellow 20-50%, red < 20%
  const maxTokens = 10
  const pct      = Math.min(100, Math.round(((user?.tokens || 0) / Math.max(maxTokens, user?.tokens || 1)) * 100))
  const barColor = pct > 50 ? '#48bb78' : pct > 20 ? '#ed8936' : '#fc8181'

  return (
    <div className="token-dashboard">
      {/* Token Balance Display */}
      <div className="token-balance-row">
        <div className="token-info">
          <span className="token-icon">🪙</span>
          <span className="token-count">{user?.tokens ?? 0}</span>
          <span className="token-text">tokens left</span>
        </div>
        <button
          className="btn btn-success buy-btn"
          onClick={() => setShowPackages(!showPackages)}
          disabled={loading}
        >
          {showPackages ? '✕ Close' : '+ Buy'}
        </button>
      </div>

      {/* Token progress bar */}
      <div className="token-bar-bg">
        <div className="token-bar-fill" style={{ width: `${pct}%`, background: barColor }} />
      </div>

      {/* Package Selector */}
      {showPackages && (
        <div className="packages">
          {/* Sale banner — only shown when discount is active */}
          {discount.enabled && (
            <div className="sale-banner">
              🔥 {discount.label} — {discount.globalBonusPercent}% Extra Tokens!
            </div>
          )}

          <p className="packages-title">Choose a package</p>
          <div className="packages-grid">
            {packages.length === 0 && (
              <p style={{ color: '#718096', fontSize: '0.8rem', gridColumn: '1/-1', textAlign: 'center' }}>
                No packages available. Check backend config.
              </p>
            )}
            {packages.map((pkg) => {
              const hasSaleBonus = discount.enabled && (pkg.bonusTokens + pkg.extraTokens) > 0
              return (
                <button
                  key={pkg.amount}
                  className={`package-card ${pkg.popular ? 'popular' : ''} ${hasSaleBonus ? 'on-sale' : ''}`}
                  onClick={() => handleBuyTokens(pkg)}
                  disabled={loading}
                >
                  {pkg.popular && !hasSaleBonus && (
                    <span className="popular-badge">Popular</span>
                  )}
                  {hasSaleBonus && (
                    <span className="sale-badge">🔥 {discount.label}</span>
                  )}

                  <span className="pkg-amount">{pkg.label}</span>

                  {hasSaleBonus ? (
                    <div className="pkg-tokens-sale">
                      <span className="pkg-tokens-original">{pkg.baseTokens} tokens</span>
                      <span className="pkg-tokens-new">{pkg.totalTokens} tokens</span>
                    </div>
                  ) : (
                    <span className="pkg-tokens">{pkg.totalTokens} tokens</span>
                  )}

                  {hasSaleBonus && (
                    <span className="pkg-bonus-detail">+{pkg.bonusTokens + pkg.extraTokens} free</span>
                  )}
                </button>
              )
            })}
          </div>
          <p className="test-mode-note">🔒 Secured by Razorpay</p>
        </div>
      )}

      {/* Status message */}
      {message && (
        <div className={`alert ${message.startsWith('✅') ? 'alert-success' : 'alert-error'}`}>
          {message}
        </div>
      )}
    </div>
  )
}
