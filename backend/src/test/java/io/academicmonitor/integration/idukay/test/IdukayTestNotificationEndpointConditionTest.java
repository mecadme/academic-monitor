package io.academicmonitor.integration.idukay.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.academicmonitor.communication.application.ManualNotificationService;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class IdukayTestNotificationEndpointConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(ControllerConfiguration.class);

    @Test
    void directEndpointControllerIsNotCreatedWhenTestEndpointsAreDisabled() {
        contextRunner.withPropertyValues("app.idukay.test-login-enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(IdukayTestNotificationController.class);
            assertThat(context.getBean(ConditionEvaluationReport.class)).isNotNull();
        });
    }

    @Test
    void directEndpointUsesTheSameFlagAsTheOtherIdukayTestEndpoint() {
        contextRunner
                .withPropertyValues("app.idukay.test-login-enabled=true", "spring.profiles.active=dev")
                .run(context -> assertThat(context).hasSingleBean(IdukayTestNotificationController.class));
    }

    @Test
    void endpointsRemainDisabledOutsideDevEvenWhenFlagIsEnabled() {
        contextRunner
                .withPropertyValues("app.idukay.test-login-enabled=true", "spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(IdukayTestNotificationController.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(IdukayTestNotificationController.class)
    static class ControllerConfiguration {

        @Bean
        AuthenticatedAcademicContext authenticatedAcademicContext() {
            return mock(AuthenticatedAcademicContext.class);
        }

        @Bean
        ManualNotificationService manualNotificationService() {
            return mock(ManualNotificationService.class);
        }

        @Bean
        IdukayDirectTestNotificationService directNotificationService() {
            return mock(IdukayDirectTestNotificationService.class);
        }
    }
}
