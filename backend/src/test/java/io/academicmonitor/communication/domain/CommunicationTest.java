package io.academicmonitor.communication.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommunicationTest {

    @Test
    void transitionsFromPendingToSent() {
        Communication communication = communication();

        communication.markSent(Instant.parse("2026-10-02T18:00:00Z"));

        assertEquals(CommunicationStatus.SENT, communication.getStatus());
    }

    @Test
    void transitionsFromPendingToFailed() {
        Communication communication = communication();

        communication.markFailed("PROVIDER_REJECTED", "Idukay returned HTTP 400");

        assertEquals(CommunicationStatus.FAILED, communication.getStatus());
    }

    private static Communication communication() {
        return new Communication(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "PLATFORM_NOTIFICATION",
                "IDUKAY",
                "Subject",
                "<p>Content</p>");
    }
}
