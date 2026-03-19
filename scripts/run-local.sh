#!/usr/bin/env bash
set -euo pipefail

echo "Starting PostgreSQL shards..."
docker compose up -d postgres-a postgres-b postgres-c

echo "Waiting for databases..."
sleep 8

echo "Starting backend..."
cd backend && mvn spring-boot:run
