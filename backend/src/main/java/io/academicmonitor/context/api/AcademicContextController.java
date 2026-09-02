package io.academicmonitor.context.api;

import io.academicmonitor.context.application.AcademicContextBootstrapService;
import io.academicmonitor.context.application.AcademicContextResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/context")
public class AcademicContextController {

    private final AcademicContextBootstrapService bootstrapService;

    public AcademicContextController(AcademicContextBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/bootstrap")
    public AcademicContextResult bootstrap() {
        return bootstrapService.bootstrap();
    }
}
