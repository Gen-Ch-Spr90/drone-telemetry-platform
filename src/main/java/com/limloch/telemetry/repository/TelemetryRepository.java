package com.limloch.telemetry.repository;

import com.limloch.telemetry.service.TelemetryCsvRow;
import com.limloch.telemetry.web.TelemetryPoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class TelemetryRepository {

    private final JdbcTemplate jdbcTemplate;

    public TelemetryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchInsert(UUID flightId, List<TelemetryCsvRow> rows) {
        String sql = """
                INSERT INTO telemetry_points (
                    flight_id, recorded_at, position, altitude_m, speed_mps, heading_deg,
                    battery_pct, satellites, signal_strength_dbm, gimbal_pitch_deg, gimbal_yaw_deg
                ) VALUES (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.batchUpdate(sql, rows, rows.size(), (statement, row) -> {
            statement.setObject(1, flightId);
            statement.setTimestamp(2, Timestamp.from(row.recordedAt().toInstant()));
            statement.setDouble(3, row.longitude());
            statement.setDouble(4, row.latitude());
            statement.setDouble(5, row.altitudeM());
            statement.setDouble(6, row.speedMps());
            statement.setDouble(7, row.headingDeg());
            statement.setDouble(8, row.batteryPct());
            statement.setInt(9, row.satellites());
            statement.setDouble(10, row.signalStrengthDbm());
            statement.setDouble(11, row.gimbalPitchDeg());
            statement.setDouble(12, row.gimbalYawDeg());
        });
    }

    public List<TelemetryPoint> findByFlightId(UUID flightId) {
        String sql = """
                SELECT recorded_at, ST_Y(position::geometry) AS latitude,
                       ST_X(position::geometry) AS longitude, altitude_m, speed_mps,
                       heading_deg, battery_pct, satellites, signal_strength_dbm,
                       gimbal_pitch_deg, gimbal_yaw_deg
                FROM telemetry_points
                WHERE flight_id = ?
                ORDER BY recorded_at
                """;
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new TelemetryPoint(
                resultSet.getTimestamp("recorded_at").toInstant().atOffset(ZoneOffset.UTC),
                resultSet.getDouble("latitude"), resultSet.getDouble("longitude"),
                resultSet.getObject("altitude_m", Double.class),
                resultSet.getObject("speed_mps", Double.class),
                resultSet.getObject("heading_deg", Double.class),
                resultSet.getObject("battery_pct", Double.class),
                resultSet.getObject("satellites", Integer.class),
                resultSet.getObject("signal_strength_dbm", Double.class),
                resultSet.getObject("gimbal_pitch_deg", Double.class),
                resultSet.getObject("gimbal_yaw_deg", Double.class)), flightId);
    }
}