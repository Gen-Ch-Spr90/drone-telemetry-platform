package com.limloch.telemetry.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TelemetryCsvRow(
        UUID flightId,
        OffsetDateTime recordedAt,
        double latitude,
        double longitude,
        double altitudeM,
        double speedMps,
        double headingDeg,
        double batteryPct,
        int satellites,
        double signalStrengthDbm,
        double gimbalPitchDeg,
        double gimbalYawDeg) {
}