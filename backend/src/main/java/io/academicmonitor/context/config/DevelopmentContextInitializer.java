package io.academicmonitor.context.config;

import io.academicmonitor.context.application.AcademicContextBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Initializes the existing local identity once at startup, never through a public API. */
@Component
@Profile("dev")
public class DevelopmentContextInitializer implements ApplicationRunner {

    private final AcademicContextBootstrapService bootstrapService;

    public DevelopmentContextInitializer(AcademicContextBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        bootstrapService.bootstrap();
    }
}
