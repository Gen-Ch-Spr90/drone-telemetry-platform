package com.limloch.telemetry.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "flight_events")
public class FlightEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 16)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    protected FlightEvent() {
    }

    public FlightEvent(UUID flightId, OffsetDateTime recordedAt,
                       String eventType, String severity, String details) {
        this.flightId = flightId;
        this.recordedAt = recordedAt;
        this.eventType = eventType;
        this.severity = severity;
        this.details = details;
    }

    public Long getId() { return id; }
    public UUID getFlightId() { return flightId; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public String getEventType() { return eventType; }
    public String getSeverity() { return severity; }
    public String getDetails() { return details; }
}