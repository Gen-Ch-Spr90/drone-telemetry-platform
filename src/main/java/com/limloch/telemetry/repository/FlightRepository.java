package com.limloch.telemetry.repository;

import com.limloch.telemetry.domain.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlightRepository extends JpaRepository<Flight, UUID> {
}