package io.academicmonitor.demo.infrastructure.platform;

import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.demo.application.DemoScenario;
import org.springframework.stereotype.Component;

@Component
public class FakeAcademicPlatformAdapterFactory {

    public AcademicPlatformPort create(DemoScenario scenario) {
        return new FakeAcademicPlatformAdapter(scenario);
    }
}
