package com.limloch.telemetry.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class GeoJsonQueryRepository {

    private final JdbcTemplate jdbc;

    public GeoJsonQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns events for a flight as a GeoJSON FeatureCollection.
     * Each event is joined to the nearest telemetry point (by time)
     * to get lat/lon.
     */
    public String findEventsAsGeoJson(UUID flightId) {
        String sql = """
                WITH event_with_position AS (
                    SELECT
                        fe.event_type,
                        fe.severity,
                        fe.recorded_at,
                        tp.position AS pos,
                        ROW_NUMBER() OVER (PARTITION BY fe.id ORDER BY ABS(EXTRACT(EPOCH FROM (tp.recorded_at - fe.recorded_at)))) AS rn
                    FROM flight_events fe
                    JOIN telemetry_points tp ON tp.flight_id = fe.flight_id
                    WHERE fe.flight_id = ?
                )
                SELECT jsonb_build_object(
                    'type', 'FeatureCollection',
                    'features', COALESCE(jsonb_agg(jsonb_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(pos::geometry)::jsonb,
                        'properties', jsonb_build_object(
                            'event_type', event_type,
                            'severity', severity,
                            'recorded_at', recorded_at
                        )
                    )), '[]'::jsonb)
                )::text
                FROM event_with_position
                WHERE rn = 1
                """;
        return jdbc.queryForObject(sql, String.class, flightId);
    }

    /**
     * Returns any geofences the flight entered as a GeoJSON FeatureCollection.
     */
    public String findIntersectedGeofencesAsGeoJson(UUID flightId) {
        String sql = """
                SELECT jsonb_build_object(
                    'type', 'FeatureCollection',
                    'features', COALESCE(jsonb_agg(DISTINCT jsonb_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(g.boundary::geometry)::jsonb,
                        'properties', jsonb_build_object(
                            'name', g.name,
                            'kind', g.kind,
                            'description', g.description
                        )
                    )), '[]'::jsonb)
                )::text
                FROM geofences g
                JOIN telemetry_points tp ON ST_Contains(g.boundary::geometry, tp.position::geometry)
                WHERE tp.flight_id = ?
                """;
        return jdbc.queryForObject(sql, String.class, flightId);
    }
}