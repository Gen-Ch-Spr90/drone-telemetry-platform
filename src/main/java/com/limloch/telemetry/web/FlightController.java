package com.limloch.telemetry.web;

import com.limloch.telemetry.repository.FlightRepository;
import com.limloch.telemetry.repository.TelemetryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightRepository flights;
    private final TelemetryRepository telemetry;

    private final com.limloch.telemetry.repository.FlightEventRepository eventRepository;

    public FlightController(FlightRepository flights, TelemetryRepository telemetry, com.limloch.telemetry.repository.FlightEventRepository eventRepository) {
        this.flights = flights;
        this.telemetry = telemetry;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public List<FlightSummary> list() {
        return flights.findAll().stream().map(FlightSummary::from).toList();
    }

    @GetMapping("/{id}")
    public FlightDetail get(@PathVariable UUID id) {
        return flights.findById(id).map(FlightDetail::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));
    }

    @GetMapping("/{id}/events")
    public List<com.limloch.telemetry.domain.FlightEvent> events(@PathVariable UUID id) {
        if (!flights.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }
        return eventRepository.findByFlightIdOrderByRecordedAtAsc(id);
    }

    @GetMapping("/{id}/points")
    public List<TelemetryPoint> points(@PathVariable UUID id) {
        if (!flights.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }
        return telemetry.findByFlightId(id);
    }
}