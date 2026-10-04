package com.limloch.telemetry.service;

import com.limloch.telemetry.domain.FlightEvent;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FlightEventDetector {

    // Thresholds chosen to mirror DJI Mavic 2 Pro warning behavior
    private static final double LOW_BATTERY_PCT = 20.0;
    private static final double CRITICAL_BATTERY_PCT = 10.0;
    private static final double WEAK_SIGNAL_DBM = -80.0;
    private static final double CRITICAL_SIGNAL_DBM = -90.0;

    /**
     * Pure computation: derives events from CSV rows only (no database access).
     */
    public List<FlightEvent> detect(UUID flightId, List<TelemetryCsvRow> rows) {
        List<FlightEvent> events = new ArrayList<>();

        for (TelemetryCsvRow row : rows) {
            // Battery events
            if (row.batteryPct() <= CRITICAL_BATTERY_PCT) {
                events.add(new FlightEvent(flightId, row.recordedAt(),
                        "LOW_BATTERY", "CRITICAL",
                        "{\"battery_pct\":" + row.batteryPct() + "}"));
            } else if (row.batteryPct() <= LOW_BATTERY_PCT) {
                events.add(new FlightEvent(flightId, row.recordedAt(),
                        "LOW_BATTERY", "WARNING",
                        "{\"battery_pct\":" + row.batteryPct() + "}"));
            }

            // Signal strength events
            if (row.signalStrengthDbm() <= CRITICAL_SIGNAL_DBM) {
                events.add(new FlightEvent(flightId, row.recordedAt(),
                        "WEAK_SIGNAL", "CRITICAL",
                        "{\"signal_dbm\":" + row.signalStrengthDbm() + "}"));
            } else if (row.signalStrengthDbm() <= WEAK_SIGNAL_DBM) {
                events.add(new FlightEvent(flightId, row.recordedAt(),
                        "WEAK_SIGNAL", "WARNING",
                        "{\"signal_dbm\":" + row.signalStrengthDbm() + "}"));
            }
        }

        return events;
    }

    /**
     * Database-dependent: given a list of geofence names the flight entered,
     * produce GEOFENCE_BREACH events.
     */
    public List<FlightEvent> detectGeofenceEvents(UUID flightId, List<String> breachedGeofences) {
        List<FlightEvent> events = new ArrayList<>();
        for (String fenceName : breachedGeofences) {
            events.add(new FlightEvent(flightId, OffsetDateTime.now(),
                    "GEOFENCE_BREACH", "CRITICAL",
                    "{\"geofence\":\"" + fenceName + "\"}"));
        }
        return events;
    }
}