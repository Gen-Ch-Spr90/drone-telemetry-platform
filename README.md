# Drone Telemetry Platform

End-to-end telemetry platform for drone flights — ingests DJI flight CSVs, stores them in PostgreSQL/PostGIS with spatial indexes, auto-detects events (low battery, weak signal, geofence breaches), serves the data via REST + GeoJSON, and visualizes it in Grafana and an interactive Leaflet map.

Built as a portfolio project combining drone operations with production-grade backend engineering.

## Features

- **CSV ingestion** — parses DJI Mavic 2 Pro telemetry logs, batch inserts into PostGIS
- **Spatial storage** — GIST-indexed geography columns, `ST_MakeLine` for flight paths
- **Event detection** — automatic flagging of low battery, weak signal, and geofence breaches
- **REST API** — flights, telemetry points, events, and GeoJSON exports
- **Grafana dashboards** — battery, altitude, signal, event counts; provisioned as code
- **Interactive map** — Leaflet with satellite imagery, live HUD, playback controls
- **Integration testing** — Testcontainers with real PostGIS containers
- **Docker Compose** — single command brings up the full stack

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 LTS |
| Framework | Spring Boot 4.1 |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Migrations | Flyway |
| Observability | Grafana 11 |
| Mapping | Leaflet 1.9 + Esri World Imagery |
| Testing | JUnit 5, Testcontainers, AssertJ |
| CI/CD | GitHub Actions |
| Container | Docker, Docker Compose |

## Running Locally