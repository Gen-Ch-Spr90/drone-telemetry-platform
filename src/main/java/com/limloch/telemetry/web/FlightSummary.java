package com.limloch.telemetry.web;

import com.limloch.telemetry.domain.Flight;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FlightSummary(UUID id, String droneModel, OffsetDateTime startedAt,
                            OffsetDateTime endedAt, Double totalDistanceM, Double maxAltitudeM) {

    public static FlightSummary from(Flight flight) {
        return new FlightSummary(flight.getId(), flight.getDroneModel(), flight.getStartedAt(),
                flight.getEndedAt(), flight.getTotalDistanceM(), flight.getMaxAltitudeM());
    }
}