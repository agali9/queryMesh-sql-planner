import { useState, useCallback, useEffect } from 'react';
import { executeQuery, explainQuery, fetchBackendHealth, fetchCatalog } from './api';
import type { QueryResponse, BackendHealth, TableMetadata } from './types';
import './App.css';

const SAMPLE_QUERIES = [
  {
    label: 'Cross-shard join (2 tables)',
    sql: 'SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id',
  },
  {
    label: '10-table join',
    sql: `SELECT u.name, r.name AS region, p.name AS product, c.name AS category,
       o.total, pay.method, inv.quantity, w.name AS warehouse, s.status
FROM users u
JOIN regions r ON u.region_id = r.id
JOIN orders o ON u.id = o.user_id
JOIN products p ON o.product_id = p.id
JOIN categories c ON p.category_id = c.id
JOIN payments pay ON o.id = pay.order_id
JOIN inventory inv ON p.id = inv.product_id
JOIN warehouses w ON inv.warehouse_id = w.id
JOIN shipments s ON o.id = s.order_id
JOIN reviews rev ON p.id = rev.product_id AND u.id = rev.user_id`,
  },
  {
    label: 'Single shard scan',
    sql: 'SELECT id, name, email FROM users WHERE region_id = 1',
  },
];

export default function App() {
  const [sql, setSql] = useState(SAMPLE_QUERIES[0].sql);
  const [result, setResult] = useState<QueryResponse | null>(null);
  const [plan, setPlan] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [backends, setBackends] = useState<Record<string, BackendHealth>>({});
  const [tables, setTables] = useState<Record<string, TableMetadata>>({});
  const [activeTab, setActiveTab] = useState<'results' | 'plan'>('results');

  useEffect(() => {
    fetchBackendHealth().then(setBackends).catch(() => {});
    fetchCatalog().then(setTables).catch(() => {});
  }, []);

  const runQuery = useCallback(async () => {
    setLoading(true);
    setError(null);
    setPlan(null);
    try {
      const res = await executeQuery(sql);
      setResult(res);
      setActiveTab('results');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Query failed');
      setResult(null);
    } finally {
      setLoading(false);
    }
  }, [sql]);

  const runExplain = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await explainQuery(sql);
      setPlan(res.physicalPlan);
      setActiveTab('plan');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Explain failed');
    } finally {
      setLoading(false);
    }
  }, [sql]);

  return (
    <div className="app">
      <header className="header">
        <div className="header-content">
          <h1>Distributed SQL Query Planner</h1>
          <p className="subtitle">Federated query execution across PostgreSQL shards</p>
        </div>
        <div className="backend-status">
          {Object.values(backends).map((b) => (
            <div key={b.backendId} className={`backend-chip ${b.healthy ? 'healthy' : 'unhealthy'}`}>
              <span className="dot" />
              {b.name}
            </div>
          ))}
        </div>
      </header>

      <main className="main">
        <aside className="sidebar">
          <h3>Sample Queries</h3>
          {SAMPLE_QUERIES.map((q) => (
            <button
              key={q.label}
              className={`sample-btn ${sql === q.sql ? 'active' : ''}`}
              onClick={() => setSql(q.sql)}
            >
              {q.label}
            </button>
          ))}

          <h3>Catalog ({Object.keys(tables).length} tables)</h3>
          <div className="catalog-list">
            {Object.entries(tables).map(([name, meta]) => (
              <div key={name} className="catalog-item">
                <span className="table-name">{name}</span>
                <span className="backend-tag">{meta.backendId}</span>
              </div>
            ))}
          </div>
        </aside>

        <section className="workspace">
          <div className="editor-panel">
            <textarea
              className="sql-editor"
              value={sql}
              onChange={(e) => setSql(e.target.value)}
              spellCheck={false}
            />
            <div className="editor-actions">
              <button className="btn primary" onClick={runQuery} disabled={loading}>
                {loading ? 'Running...' : 'Execute'}
              </button>
              <button className="btn secondary" onClick={runExplain} disabled={loading}>
                Explain Plan
              </button>
            </div>
          </div>

          <div className="results-panel">
            <div className="tabs">
              <button
                className={`tab ${activeTab === 'results' ? 'active' : ''}`}
                onClick={() => setActiveTab('results')}
              >
                Results
              </button>
              <button
                className={`tab ${activeTab === 'plan' ? 'active' : ''}`}
                onClick={() => setActiveTab('plan')}
              >
                Query Plan
              </button>
            </div>

            {error && <div className="error-banner">{error}</div>}

            {activeTab === 'results' && result && (
              <div className="results-content">
                <div className="metrics-bar">
                  <span>{result.rows.length} rows</span>
                  <span>{result.executionTimeMs}ms</span>
                  <span>Backends: {result.backendsUsed.join(', ')}</span>
                  {result.degraded && (
                    <span className="degraded-badge">Degraded</span>
                  )}
                </div>
                {result.failures.length > 0 && (
                  <div className="failure-list">
                    {result.failures.map((f) => (
                      <div key={f.backendId} className="failure-item">
                        {f.backendName}: {f.errorMessage} ({f.phase})
                      </div>
                    ))}
                  </div>
                )}
                <div className="table-wrapper">
                  <table>
                    <thead>
                      <tr>
                        {result.columns.map((col) => (
                          <th key={col}>{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {result.rows.map((row, i) => (
                        <tr key={i}>
                          {result.columns.map((col) => (
                            <td key={col}>{String(row[col] ?? '')}</td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {activeTab === 'plan' && plan && (
              <pre className="plan-output">{plan}</pre>
            )}

            {activeTab === 'results' && !result && !error && !loading && (
              <div className="empty-state">Execute a query to see results</div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
