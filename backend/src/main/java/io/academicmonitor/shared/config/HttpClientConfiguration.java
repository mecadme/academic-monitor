package io.academicmonitor.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class HttpClientConfiguration {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
