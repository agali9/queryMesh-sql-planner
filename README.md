# sql-planner

a SQL query engine that runs queries across 3 separate PostgreSQL databases. You write normal SQL and it figures out which DB has which tables.

I wanted to learn how query planning and distributed joins work without using something huge like Presto.

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

Java + Spring Boot backend, React frontend.