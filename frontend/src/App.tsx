import { useEffect, useState } from 'react'
import { api, type HealthStatus } from './api'
import './App.css'

function App() {
  const [health, setHealth] = useState<HealthStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadHealth = async () => {
      try {
        const status = await api.getHealth()
        setHealth(status)
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unable to reach backend')
      } finally {
        setLoading(false)
      }
    }

    void loadHealth()
  }, [])

  return (
    <main className="page-shell">
      <section className="hero-card">
        <p className="eyebrow">Buck Watch</p>
        <h1>Personal finance, planned with clarity.</h1>
        <p className="lede">
          This is the startup shell for a financial dashboard that will analyze spending,
          forecast cash flow, and model financial decisions.
        </p>

        <div className="status-panel" aria-live="polite">
          <h2>Backend health</h2>
          {loading && <p>Checking backend status…</p>}
          {!loading && error && <p className="status-error">{error}</p>}
          {!loading && health && (
            <p className="status-ok">
              {health.status} • {health.service}
            </p>
          )}
        </div>
      </section>

      <section className="feature-grid">
        <article>
          <h3>Spending insights</h3>
          <p>Review transaction patterns and identify cash flow trends.</p>
        </article>
        <article>
          <h3>Forecasting</h3>
          <p>Model monthly balances and future obligations before they happen.</p>
        </article>
        <article>
          <h3>Decision simulation</h3>
          <p>Compare scenarios for rent, savings, and large purchases.</p>
        </article>
      </section>
    </main>
  )
}

export default App
