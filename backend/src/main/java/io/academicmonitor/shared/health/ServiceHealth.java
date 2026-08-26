package io.academicmonitor.shared.health;

public record ServiceHealth(String name, ServiceState status, String message) {

    public static ServiceHealth up(String name, String message) {
        return new ServiceHealth(name, ServiceState.UP, message);
    }

    public static ServiceHealth degraded(String name, String message) {
        return new ServiceHealth(name, ServiceState.DEGRADED, message);
    }

    boolean isUp() {
        return ServiceState.UP.equals(status);
    }
}
