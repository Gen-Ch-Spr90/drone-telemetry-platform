package com.limloch.telemetry.service;

import com.limloch.telemetry.repository.FlightRepository;
import com.limloch.telemetry.repository.TelemetryRepository;
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

    public FlightCsvIngestionService(FlightRepository flights, TelemetryRepository telemetry,
                                     IngestionAuditService audit, CsvParser parser, FlightMapper mapper) {
        this.flights = flights;
        this.telemetry = telemetry;
        this.audit = audit;
        this.parser = parser;
        this.mapper = mapper;
    }

    @Transactional
    public UUID ingest(Path csvPath) {
        long auditId = audit.start(csvPath.getFileName().toString());
        try {
            var rows = parser.parse(csvPath);
            UUID flightId = rows.getFirst().flightId();
            flights.save(mapper.from(csvPath, rows));
            telemetry.batchInsert(flightId, rows);
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