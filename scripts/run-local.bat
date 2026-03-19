@echo off
echo Starting PostgreSQL shards...
docker compose up -d postgres-a postgres-b postgres-c

echo Waiting for databases to be ready...
timeout /t 8 /nobreak > nul

echo Starting backend...
cd backend
mvn spring-boot:run
