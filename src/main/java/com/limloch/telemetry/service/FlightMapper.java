package com.limloch.telemetry.service;

import com.limloch.telemetry.domain.Flight;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class FlightMapper {

    public Flight from(Path source, List<TelemetryCsvRow> rows) {
        TelemetryCsvRow first = rows.getFirst();
        TelemetryCsvRow last = rows.getLast();
        double distance = 0;
        double maxAltitude = first.altitudeM();
        for (int index = 1; index < rows.size(); index++) {
            TelemetryCsvRow previous = rows.get(index - 1);
            TelemetryCsvRow current = rows.get(index);
            distance += haversine(previous.latitude(), previous.longitude(),
                    current.latitude(), current.longitude());
            maxAltitude = Math.max(maxAltitude, current.altitudeM());
        }
        return new Flight(first.flightId(), "DJI Mavic 2 Pro", null, first.recordedAt(),
                last.recordedAt(), distance, maxAltitude, source.getFileName().toString());
    }

    private double haversine(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusM = 6_371_000;
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double term = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 2 * earthRadiusM * Math.asin(Math.sqrt(term));
    }
}