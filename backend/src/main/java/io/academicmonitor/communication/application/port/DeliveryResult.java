package io.academicmonitor.communication.application.port;

public record DeliveryResult(boolean delivered, String failureCode, String failureReason) {

    public static DeliveryResult sent() {
        return new DeliveryResult(true, null, null);
    }

    public static DeliveryResult failed(String failureCode, String failureReason) {
        if (failureCode == null || failureCode.isBlank() || failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("delivery failure details are required");
        }
        return new DeliveryResult(false, failureCode.trim(), failureReason.trim());
    }
}
