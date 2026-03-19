export interface QueryResponse {
  rows: Record<string, unknown>[];
  columns: string[];
  backendsUsed: string[];
  executionTimeMs: number;
  degraded: boolean;
  failures: BackendFailure[];
}

export interface BackendFailure {
  backendId: string;
  backendName: string;
  errorMessage: string;
  phase: string;
}

export interface BackendHealth {
  backendId: string;
  name: string;
  healthy: boolean;
  lastError: string | null;
}

export interface TableMetadata {
  name: string;
  backendId: string;
  columns: string[];
}

export interface ExplainResponse {
  sql: string;
  logicalPlan: string;
  physicalPlan: string;
  involvedBackends: string[];
}
