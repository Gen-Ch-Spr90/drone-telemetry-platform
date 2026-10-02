package com.limloch.telemetry.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesGeneratedTelemetryFormat() throws Exception {
        Path csv = temporaryDirectory.resolve("flight.csv");
        Files.writeString(csv, """
                flight_id,recorded_at,lat,lon,altitude_m,speed_mps,heading_deg,battery_pct,satellites,signal_strength_dbm,gimbal_pitch_deg,gimbal_yaw_deg
                550e8400-e29b-41d4-a716-446655440000,2026-01-01T12:00:00Z,32.7767,-96.7970,42.5,8.2,180.0,95.0,15,-62.0,-20.0,5.0
                """);

        var rows = parser.parse(csv);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().latitude()).isEqualTo(32.7767);
        assertThat(rows.getFirst().altitudeM()).isEqualTo(42.5);
    }

    @Test
    void rejectsEmptyTelemetryFile() throws Exception {
        Path csv = temporaryDirectory.resolve("empty.csv");
        Files.writeString(csv, "flight_id,recorded_at,lat,lon,altitude_m,speed_mps,heading_deg,battery_pct,satellites,signal_strength_dbm,gimbal_pitch_deg,gimbal_yaw_deg\n");

        assertThatThrownBy(() -> parser.parse(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV contains no telemetry rows");
    }
}