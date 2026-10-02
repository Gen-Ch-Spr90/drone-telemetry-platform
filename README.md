# Drone Telemetry Platform

Spring Boot service for ingesting drone telemetry CSV files into PostgreSQL/PostGIS and querying flights through a REST API.

## Requirements

- Java 25 LTS
- Docker with Compose

## Run locally

```bash
cp .env.example .env
docker compose up -d postgres
./mvnw spring-boot:run
```

Generate sample data and upload it:

```bash
mkdir -p samples
python3 tools:synthetic_flight_generator.py
curl -F "file=@samples/<generated-file>.csv" http://localhost:8080/api/ingestions
```

Query the API:

```bash
curl http://localhost:8080/api/flights
curl http://localhost:8080/api/flights/<flight-id>
curl http://localhost:8080/api/flights/<flight-id>/points
```

Run tests with `./mvnw clean test`.