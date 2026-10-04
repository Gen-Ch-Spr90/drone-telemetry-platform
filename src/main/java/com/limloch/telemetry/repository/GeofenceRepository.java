package com.limloch.telemetry.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class GeofenceRepository {

    private final JdbcTemplate jdbc;

    public GeofenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the names of any geofences that contain any telemetry point of the given flight.
     * Uses PostGIS ST_Contains against a GIST-indexed geography column.
     */
    public java.util.List<String> findBreachedGeofences(UUID flightId) {
        String sql = """
                SELECT DISTINCT g.name
                FROM geofences g
                JOIN telemetry_points tp ON ST_Contains(g.boundary::geometry, tp.position::geometry)
                WHERE tp.flight_id = ?
                """;
        return jdbc.queryForList(sql, String.class, flightId);
    }
}