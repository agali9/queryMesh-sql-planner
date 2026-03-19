import type { QueryResponse, BackendHealth, TableMetadata, ExplainResponse } from './types';

const BASE = '/api';

export async function executeQuery(sql: string): Promise<QueryResponse> {
  const res = await fetch(`${BASE}/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sql }),
  });
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || 'Query execution failed');
  }
  return res.json();
}

export async function explainQuery(sql: string): Promise<ExplainResponse> {
  const res = await fetch(`${BASE}/explain`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sql }),
  });
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || 'Explain failed');
  }
  return res.json();
}

export async function fetchBackendHealth(): Promise<Record<string, BackendHealth>> {
  const res = await fetch(`${BASE}/backends/health`);
  return res.json();
}

export async function fetchCatalog(): Promise<Record<string, TableMetadata>> {
  const res = await fetch(`${BASE}/catalog/tables`);
  return res.json();
}
