package io.academicmonitor.identity.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.academicmonitor.identity.application.AcademicMonitorPrincipal;
import io.academicmonitor.identity.application.AccessTokenService;
import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.institution.domain.InstitutionRole;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class AuthSecurityConfiguration {
    @Bean
    public Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    public SecretKey jwtSigningKey(AuthProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder()
                    .decode(properties.jwtSecretBase64() == null ? "" : properties.jwtSecretBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_AUTH_JWT_SECRET_BASE64 must be valid Base64");
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("APP_AUTH_JWT_SECRET_BASE64 must contain at least 32 random bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, new JwtIssuerValidator(AccessTokenService.ISSUER), jwt -> {
                    try {
                        principal(jwt);
                        if (jwt.getIssuedAt() == null
                                || jwt.getExpiresAt() == null
                                || jwt.getId() == null
                                || jwt.getIssuedAt().isAfter(clock.instant())
                                || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())) {
                            throw new IllegalArgumentException("Missing access claims");
                        }
                        UUID.fromString(jwt.getId());
                        return OAuth2TokenValidatorResult.success();
                    } catch (IllegalArgumentException | NullPointerException exception) {
                        return OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Invalid access claims", null));
                    }
                }));
        return decoder;
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(AuthProperties properties, Environment environment) {
        if (!properties.cookieSecure() && !environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            throw new IllegalStateException("Authentication cookies must be Secure outside dev and test profiles");
        }
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(
                cookie -> cookie.path("/").secure(properties.cookieSecure()).sameSite(properties.cookieSameSite()));
        return repository;
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(JwtDecoder decoder) {
        var provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(jwt -> {
            var principal = principal(jwt);
            return UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    null,
                    List.of(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + principal.institutionRole().name()),
                            new SimpleGrantedAuthority(
                                    "SYSTEM_" + principal.systemRole().name())));
        });
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationProvider provider,
            CookieCsrfTokenRepository csrfRepository,
            Environment environment)
            throws Exception {
        AuthenticationEntryPoint unauthorized =
                (request, response, exception) -> problem(response, 401, "Authentication required");
        var bearerFilter = new BearerTokenAuthenticationFilter(new ProviderManager(provider));
        bearerFilter.setBearerTokenResolver(new CookieBearerTokenResolver());
        bearerFilter.setAuthenticationEntryPoint(unauthorized);
        bearerFilter.setSecurityContextRepository(new NullSecurityContextRepository());

        http.cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfRepository))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request, response, exception) -> problem(response, 403, "Access denied")))
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(
                                    "/api/v1/auth/csrf",
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/refresh",
                                    "/api/v1/auth/logout",
                                    "/actuator/health",
                                    "/actuator/health/**",
                                    "/error")
                            .permitAll();
                    authorize.requestMatchers("/api/v1/auth/me").authenticated();
                    if (environment.acceptsProfiles(Profiles.of("dev"))) {
                        authorize
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .authenticated();
                    }
                    authorize
                            .requestMatchers("/api/v1/**")
                            .hasRole("TEACHER")
                            .anyRequest()
                            .denyAll();
                });
        // The OAuth resource-server configurer exempts bearer requests from CSRF. Register its
        // standard filter directly so cookie authentication always retains SPA CSRF protection.
        http.addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static AcademicMonitorPrincipal principal(Jwt jwt) {
        return new AcademicMonitorPrincipal(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("institutionId")),
                SystemRole.valueOf(jwt.getClaimAsString("systemRole")),
                InstitutionRole.valueOf(jwt.getClaimAsString("institutionRole")));
    }

    private static void problem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
