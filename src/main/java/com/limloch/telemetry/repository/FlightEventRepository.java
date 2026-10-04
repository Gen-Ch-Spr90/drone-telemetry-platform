package com.limloch.telemetry.repository;

import com.limloch.telemetry.domain.FlightEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlightEventRepository extends JpaRepository<FlightEvent, Long> {

    List<FlightEvent> findByFlightIdOrderByRecordedAtAsc(UUID flightId);
}