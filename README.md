# QueryMesh (sql-planner)

QueryMesh is a federated SQL query planner that runs queries across three separate PostgreSQL databases. You write normal SQL and it figures out which DB has which tables, plans the query, and joins across databases when it has to.

## How it works

- **Parsing:** JSqlParser turns a SELECT into a sealed logical plan.
- **Planning:** the logical plan becomes backend-specific physical plans, with partition-aware planning, join pushdown when both tables live on the same backend, and cross-shard hash joins when they don't.
- **Execution:** a Volcano-style iterator engine streams rows from each backend over JDBC (fetch-size streaming) through HikariCP connection pools.
- **Resilience:** backend health tracking and partial-failure reporting.
- **API:** REST endpoints for query, explain, catalog, and health (below).

Stack: Java 21, Spring Boot 3.3, PostgreSQL 16, React frontend.

## Results

**Parallel backend fetches.** Fetching from backends concurrently instead of one at a time, on a 1:1 join of `users` (backend A) with `orders` (backend B). Median engine execution time, 1 warmup plus 5 measured runs per scale:

| Rows per backend | Serial | Parallel | Speedup |
|---:|---:|---:|---:|
| 10K | 64 ms | 42 ms | 1.52x |
| 100K | 506 ms | 337 ms | 1.50x |
| 1M | 4,864 ms | 3,456 ms | 1.41x |

Times are end-to-end `ExecutionEngine.execute`, not HTTP latency.

**Overlapping waits.** With an injected 400 ms connection delay on each of two backends, engine time fell from about 823 ms (serial) to 413 ms (parallel).

**Tests:** 14 JUnit unit tests and the JDBC integration tests pass.

## Setup

Data is split across 3 Postgres instances:

- **A** (5432): users, regions, reviews
- **B** (5433): orders, products, categories, payments
- **C** (5434): inventory, warehouses, shipments

## Run it

Need Java 21, Maven, Docker.

```bash
docker compose up -d postgres-a postgres-b postgres-c

cd backend
mvn spring-boot:run
```

Frontend (optional):

```bash
cd frontend
npm install
npm run dev
```

API: [http://localhost:8080](http://localhost:8080)  
Frontend: [http://localhost:3000](http://localhost:3000)

Or run everything in Docker:

```bash
cd backend && mvn package -DskipTests
cd .. && docker compose up --build
```

## API

- `POST /api/query` — run a query
- `POST /api/explain` — see the plan
- `GET /api/catalog/tables` — list tables
- `GET /api/backends/health` — check if DBs are up

## Tests

```bash
cd backend && mvn test
```

## Why I built it

I wanted to learn how query planning and distributed joins work without using something huge like Presto.
