package com.limloch.telemetry.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlightMapperTest {

    @Test
    void derivesFlightMetricsFromTelemetry() {
        UUID flightId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T12:00:00Z");
        var first = row(flightId, start, 32.7767, -96.7970, 10);
        var second = row(flightId, start.plusSeconds(1), 32.7777, -96.7970, 50);

        var flight = new FlightMapper().from(Path.of("flight.csv"), List.of(first, second));

        assertThat(flight.getId()).isEqualTo(flightId);
        assertThat(flight.getStartedAt()).isEqualTo(start);
        assertThat(flight.getEndedAt()).isEqualTo(start.plusSeconds(1));
        assertThat(flight.getMaxAltitudeM()).isEqualTo(50);
        assertThat(flight.getTotalDistanceM()).isBetween(111.0, 112.0);
    }

    private TelemetryCsvRow row(UUID id, OffsetDateTime time, double latitude,
                                double longitude, double altitude) {
        return new TelemetryCsvRow(id, time, latitude, longitude, altitude,
                5, 90, 80, 12, -60, -20, 0);
    }
}