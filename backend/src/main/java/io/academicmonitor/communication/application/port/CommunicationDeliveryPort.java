package io.academicmonitor.communication.application.port;

public interface CommunicationDeliveryPort {
    String providerCode();

    DeliveryResult send(CommunicationDeliveryRequest request);
}
