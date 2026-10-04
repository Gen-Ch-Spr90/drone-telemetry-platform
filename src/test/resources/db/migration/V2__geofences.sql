CREATE TABLE geofences (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    kind        VARCHAR(32)  NOT NULL,   -- RESTRICTED, AIRPORT, SCHOOL, etc.
    boundary    GEOGRAPHY(POLYGON, 4326) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_geofences_boundary ON geofences USING GIST (boundary);