package io.academicmonitor.integration.idukay.test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/idukay")
@ConditionalOnProperty(prefix = "app.idukay", name = "test-login-enabled", havingValue = "true")
public class IdukayTestLoginController {

    private final IdukayLoginOrchestrator orchestrator;

    public IdukayTestLoginController(IdukayLoginOrchestrator orchestrator) {

        this.orchestrator = orchestrator;
    }

    @PostMapping("/test-login")
    public ResponseEntity<IdukayTestLoginResponse> testLogin(@RequestBody IdukayTestLoginRequest request) {

        return ResponseEntity.ok(orchestrator.testLogin(request));
    }
}
