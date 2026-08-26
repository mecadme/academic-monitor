package io.academicmonitor.shared.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    private final SystemHealthService systemHealthService;

    HealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping
    ResponseEntity<SystemHealthResponse> health() {
        return ResponseEntity.ok(systemHealthService.current());
    }
}
