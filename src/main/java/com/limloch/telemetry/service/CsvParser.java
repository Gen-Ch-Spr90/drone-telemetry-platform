package com.limloch.telemetry.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CsvParser {

    public List<TelemetryCsvRow> parse(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            var format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get();
            List<TelemetryCsvRow> rows = new ArrayList<>();
            UUID flightId = null;
            for (CSVRecord record : format.parse(reader)) {
                TelemetryCsvRow row = toRow(record);
                if (flightId != null && !flightId.equals(row.flightId())) {
                    throw new IllegalArgumentException("CSV contains more than one flight_id");
                }
                flightId = row.flightId();
                rows.add(row);
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV contains no telemetry rows");
            }
            return List.copyOf(rows);
        }
    }

    private TelemetryCsvRow toRow(CSVRecord record) {
        return new TelemetryCsvRow(
                UUID.fromString(record.get("flight_id")),
                OffsetDateTime.parse(record.get("recorded_at")),
                Double.parseDouble(record.get("lat")),
                Double.parseDouble(record.get("lon")),
                Double.parseDouble(record.get("altitude_m")),
                Double.parseDouble(record.get("speed_mps")),
                Double.parseDouble(record.get("heading_deg")),
                Double.parseDouble(record.get("battery_pct")),
                Integer.parseInt(record.get("satellites")),
                Double.parseDouble(record.get("signal_strength_dbm")),
                Double.parseDouble(record.get("gimbal_pitch_deg")),
                Double.parseDouble(record.get("gimbal_yaw_deg")));
    }
}