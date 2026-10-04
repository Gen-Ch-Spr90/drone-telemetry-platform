package com.limloch.telemetry.web;

import com.limloch.telemetry.repository.FlightRepository;
import com.limloch.telemetry.repository.GeoJsonQueryRepository;
import com.limloch.telemetry.repository.TelemetryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flights")
public class GeoJsonController {

    private final FlightRepository flights;
    private final TelemetryRepository telemetry;
    private final GeoJsonQueryRepository geoJson;

    public GeoJsonController(FlightRepository flights,
                             TelemetryRepository telemetry,
                             GeoJsonQueryRepository geoJson) {
        this.flights = flights;
        this.telemetry = telemetry;
        this.geoJson = geoJson;
    }

    /**
     * Combined GeoJSON: flight path, events, and intersected geofences.
     * Returns a single object with three FeatureCollections so the client
     * can render everything with one fetch.
     */
    @GetMapping(value = "/{id}/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> geoJson(@PathVariable UUID id) {
        if (!flights.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }

        String path = telemetry.findFlightPathAsGeoJson(id);
        String events = geoJson.findEventsAsGeoJson(id);
        String geofences = geoJson.findIntersectedGeofencesAsGeoJson(id);

        String body = """
            {"path":%s,"events":%s,"geofences":%s}
            """.formatted(
                wrapLineString(path), events, geofences);

        return ResponseEntity.ok(body);
    }

    private String wrapLineString(String lineStringGeoJson) {
        if (lineStringGeoJson == null) {
            return "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{}}";
        }
        return "{\"type\":\"Feature\",\"properties\":{},\"geometry\":" + lineStringGeoJson + "}";
    }
}