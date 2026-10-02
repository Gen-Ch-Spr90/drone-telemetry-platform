package com.limloch.telemetry.web;

import com.limloch.telemetry.domain.Flight;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FlightDetail(UUID id, String droneModel, String pilot, OffsetDateTime startedAt,
                           OffsetDateTime endedAt, Double totalDistanceM, Double maxAltitudeM,
                           String sourceFile) {

    public static FlightDetail from(Flight flight) {
        return new FlightDetail(flight.getId(), flight.getDroneModel(), flight.getPilot(),
                flight.getStartedAt(), flight.getEndedAt(), flight.getTotalDistanceM(),
                flight.getMaxAltitudeM(), flight.getSourceFile());
    }
}