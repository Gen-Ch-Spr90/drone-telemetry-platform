package com.limloch.telemetry.web;

import com.limloch.telemetry.service.FlightCsvIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingestions")
public class IngestionController {

    private final FlightCsvIngestionService ingestionService;

    public IngestionController(FlightCsvIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> ingest(@RequestParam("file") MultipartFile file) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("telemetry-", ".csv");
            file.transferTo(temporaryFile);
            return Map.of("flightId", ingestionService.ingest(temporaryFile));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                }
            }
        }
    }
}