package io.academicmonitor.context.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
@EnableConfigurationProperties(AcademicContextProperties.class)
class AcademicContextConfiguration {}
