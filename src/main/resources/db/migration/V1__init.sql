CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE flights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drone_model VARCHAR(64) NOT NULL,
    pilot VARCHAR(128),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    home_point GEOGRAPHY(POINT, 4326),
    total_distance_m DOUBLE PRECISION,
    max_altitude_m DOUBLE PRECISION,
    source_file VARCHAR(512),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE telemetry_points (
    id BIGSERIAL PRIMARY KEY,
    flight_id UUID NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL,
    position GEOGRAPHY(POINT, 4326) NOT NULL,
    altitude_m DOUBLE PRECISION,
    speed_mps DOUBLE PRECISION,
    heading_deg DOUBLE PRECISION,
    battery_pct DOUBLE PRECISION,
    satellites INT,
    signal_strength_dbm DOUBLE PRECISION,
    gimbal_pitch_deg DOUBLE PRECISION,
    gimbal_yaw_deg DOUBLE PRECISION
);

CREATE INDEX idx_telemetry_flight_time ON telemetry_points (flight_id, recorded_at);
CREATE INDEX idx_telemetry_position ON telemetry_points USING GIST (position);

CREATE TABLE flight_events (
    id BIGSERIAL PRIMARY KEY,
    flight_id UUID NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    details JSONB
);

CREATE INDEX idx_events_flight ON flight_events (flight_id, recorded_at);

CREATE TABLE ingestion_audit (
    id BIGSERIAL PRIMARY KEY,
    source_file VARCHAR(512),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    points_ingested INT,
    status VARCHAR(16) NOT NULL,
    error_message TEXT
);