package com.limloch.telemetry.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;

@Entity
@Table(name = "flights")
public class Flight {

    @Id
    private UUID id;
    private String droneModel;
    private String pilot;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    @Column(name = "total_distance_m")
    private Double totalDistanceM;

    @Column(name = "max_altitude_m")
    private Double maxAltitudeM;
    private String sourceFile;

    protected Flight() {
    }

    public Flight(UUID id, String droneModel, String pilot, OffsetDateTime startedAt,
                  OffsetDateTime endedAt, Double totalDistanceM, Double maxAltitudeM,
                  String sourceFile) {
        this.id = id;
        this.droneModel = droneModel;
        this.pilot = pilot;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.totalDistanceM = totalDistanceM;
        this.maxAltitudeM = maxAltitudeM;
        this.sourceFile = sourceFile;
    }

    public UUID getId() { return id; }
    public String getDroneModel() { return droneModel; }
    public String getPilot() { return pilot; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public Double getTotalDistanceM() { return totalDistanceM; }
    public Double getMaxAltitudeM() { return maxAltitudeM; }
    public String getSourceFile() { return sourceFile; }
}