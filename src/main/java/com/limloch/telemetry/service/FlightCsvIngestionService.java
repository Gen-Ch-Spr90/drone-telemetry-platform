package com.limloch.telemetry.service;

import com.limloch.telemetry.repository.FlightRepository;
import com.limloch.telemetry.repository.TelemetryRepository;
import com.limloch.telemetry.repository.FlightEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FlightCsvIngestionService {

    private final FlightRepository flights;
    private final TelemetryRepository telemetry;
    private final IngestionAuditService audit;
    private final CsvParser parser;
    private final FlightMapper mapper;

    private final FlightEventDetector eventDetector;
    private final FlightEventRepository eventRepository;

    public FlightCsvIngestionService(FlightRepository flights, TelemetryRepository telemetry,
                                     IngestionAuditService audit, CsvParser parser,
                                     FlightMapper mapper, FlightEventDetector eventDetector,
                                     FlightEventRepository eventRepository) {
        this.flights = flights;
        this.telemetry = telemetry;
        this.audit = audit;
        this.parser = parser;
        this.mapper = mapper;
        this.eventDetector = eventDetector;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public UUID ingest(Path csvPath) {
        long auditId = audit.start(csvPath.getFileName().toString());
        try {
            var rows = parser.parse(csvPath);
            UUID flightId = rows.getFirst().flightId();
            flights.saveAndFlush(mapper.from(csvPath, rows));
            telemetry.batchInsert(flightId, rows);
            var events = eventDetector.detect(flightId, rows);
            if (!events.isEmpty()) {
                eventRepository.saveAll(events);
            }
            audit.success(auditId, rows.size());
            return flightId;
        } catch (IOException exception) {
            audit.failure(auditId, exception.getMessage());
            throw new UncheckedIOException(exception);
        } catch (RuntimeException exception) {
            audit.failure(auditId, exception.getMessage());
            throw exception;
        }
    }
}