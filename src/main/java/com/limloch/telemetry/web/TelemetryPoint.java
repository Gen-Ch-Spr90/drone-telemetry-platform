package com.limloch.telemetry.web;

import java.time.OffsetDateTime;

public record TelemetryPoint(
        OffsetDateTime recordedAt,
        double latitude,
        double longitude,
        Double altitudeM,
        Double speedMps,
        Double headingDeg,
        Double batteryPct,
        Integer satellites,
        Double signalStrengthDbm,
        Double gimbalPitchDeg,
        Double gimbalYawDeg) {
}