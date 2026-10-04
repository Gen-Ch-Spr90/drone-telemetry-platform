package com.limloch.telemetry;

import com.limloch.telemetry.service.FlightCsvIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
                    "/docker-entrypoint-initdb.d/V1__init.sql");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired FlightCsvIngestionService ingestionService;
    @Autowired JdbcTemplate jdbcTemplate;

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