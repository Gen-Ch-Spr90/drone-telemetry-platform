package com.limloch.telemetry;

import com.limloch.telemetry.repository.TelemetryRepository;
import com.limloch.telemetry.service.FlightCsvIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class IngestionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("telemetry")
            .withUsername("telemetry")
            .withPassword("telemetry")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("db/migration/V1__init.sql"),
                    "/docker-entrypoint-initdb.d/V1__init.sql")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("db/migration/V2__geofences.sql"),
                    "/docker-entrypoint-initdb.d/V2__geofences.sql");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired FlightCsvIngestionService ingestionService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TelemetryRepository telemetryRepository;

    @Test
    void ingestsCsvEndToEnd() throws IOException {
        Path csv = writeSampleCsv();

        UUID flightId = ingestionService.ingest(csv);

        Integer flightCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flights WHERE id = ?", Integer.class, flightId);
        assertThat(flightCount).isEqualTo(1);

        Integer pointCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM telemetry_points WHERE flight_id = ?",
                Integer.class, flightId);
        assertThat(pointCount).isEqualTo(3);

        Double lon = jdbcTemplate.queryForObject("""
                SELECT ST_X(position::geometry) FROM telemetry_points
                WHERE flight_id = ? ORDER BY recorded_at LIMIT 1
                """, Double.class, flightId);
        assertThat(lon).isCloseTo(-96.7970, org.assertj.core.data.Offset.offset(0.0001));

        String status = jdbcTemplate.queryForObject("""
                SELECT status FROM ingestion_audit
                WHERE source_file = ? ORDER BY id DESC LIMIT 1
                """, String.class, csv.getFileName().toString());
        assertThat(status).isEqualTo("SUCCESS");
    }

    @Test
    void detectsLowBatteryAndWeakSignalEvents() throws IOException {
        Path csv = Files.createTempFile("event-detect-", ".csv");
        Files.writeString(csv, """
                flight_id,recorded_at,lat,lon,altitude_m,speed_mps,heading_deg,battery_pct,satellites,signal_strength_dbm,gimbal_pitch_deg,gimbal_yaw_deg
                660e8400-e29b-41d4-a716-446655440001,2026-01-01T13:00:00Z,32.7767,-96.7970,42.5,8.2,180.0,50.0,15,-60.0,-20.0,5.0
                660e8400-e29b-41d4-a716-446655440001,2026-01-01T13:00:01Z,32.7770,-96.7970,45.0,8.4,180.0,15.0,15,-75.0,-20.0,5.0
                660e8400-e29b-41d4-a716-446655440001,2026-01-01T13:00:02Z,32.7773,-96.7970,47.5,8.5,180.0,8.0,15,-95.0,-20.0,5.0
                """);

        UUID flightId = ingestionService.ingest(csv);

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flight_events WHERE flight_id = ?",
                Integer.class, flightId);
        assertThat(eventCount).isEqualTo(3);

        Integer criticalBatteryCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flight_events
                WHERE flight_id = ? AND event_type = 'LOW_BATTERY' AND severity = 'CRITICAL'
                """, Integer.class, flightId);
        assertThat(criticalBatteryCount).isEqualTo(1);
    }

    @Test
    void detectsGeofenceBreach() throws IOException {
        jdbcTemplate.update("""
                INSERT INTO geofences (name, kind, boundary, description)
                VALUES ('Test Flight Zone', 'RESTRICTED',
                        ST_GeogFromText('POLYGON((-96.80 32.77, -96.79 32.77, -96.79 32.78, -96.80 32.78, -96.80 32.77))'),
                        'Test fence')
                """);

        Path csv = Files.createTempFile("geofence-", ".csv");
        Files.writeString(csv, """
                flight_id,recorded_at,lat,lon,altitude_m,speed_mps,heading_deg,battery_pct,satellites,signal_strength_dbm,gimbal_pitch_deg,gimbal_yaw_deg
                770e8400-e29b-41d4-a716-446655440002,2026-01-01T14:00:00Z,32.7767,-96.7970,42.5,8.2,180.0,95.0,15,-60.0,-20.0,5.0
                """);

        UUID flightId = ingestionService.ingest(csv);

        Integer geofenceEventCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flight_events
                WHERE flight_id = ? AND event_type = 'GEOFENCE_BREACH'
                """, Integer.class, flightId);
        assertThat(geofenceEventCount).isEqualTo(1);
    }

    @Test
    void servesFlightPathAsGeoJson() throws IOException {
        Path csv = writeSampleCsv();
        UUID flightId = ingestionService.ingest(csv);

        String pathGeoJson = telemetryRepository.findFlightPathAsGeoJson(flightId);

        assertThat(pathGeoJson).isNotNull();
        assertThat(pathGeoJson).contains("\"type\":\"LineString\"");
        assertThat(pathGeoJson).contains("-96.797");
        assertThat(pathGeoJson).contains("32.7767");
        assertThat(pathGeoJson).contains("-96.797");
    }

    private Path writeSampleCsv() throws IOException {
        Path csv = Files.createTempFile("integration-", ".csv");
        Files.writeString(csv, """
                flight_id,recorded_at,lat,lon,altitude_m,speed_mps,heading_deg,battery_pct,satellites,signal_strength_dbm,gimbal_pitch_deg,gimbal_yaw_deg
                550e8400-e29b-41d4-a716-446655440000,2026-01-01T12:00:00Z,32.7767,-96.7970,42.5,8.2,180.0,95.0,15,-62.0,-20.0,5.0
                550e8400-e29b-41d4-a716-446655440000,2026-01-01T12:00:01Z,32.7770,-96.7970,45.0,8.4,180.0,94.5,15,-62.0,-20.0,5.0
                550e8400-e29b-41d4-a716-446655440000,2026-01-01T12:00:02Z,32.7773,-96.7970,47.5,8.5,180.0,94.0,15,-63.0,-20.0,5.0
                """);
        return csv;
    }
}