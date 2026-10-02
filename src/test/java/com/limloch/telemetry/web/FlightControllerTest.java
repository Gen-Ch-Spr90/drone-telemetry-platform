package com.limloch.telemetry.web;

import com.limloch.telemetry.repository.FlightRepository;
import com.limloch.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlightControllerTest {

    @Test
    void returnsNotFoundForMissingFlight() {
        FlightRepository flights = mock(FlightRepository.class);
        UUID flightId = UUID.randomUUID();
        when(flights.findById(flightId)).thenReturn(Optional.empty());
        FlightController controller = new FlightController(flights, mock(TelemetryRepository.class));

        assertThatThrownBy(() -> controller.get(flightId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}