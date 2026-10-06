package io.academicmonitor.identity.application;

import io.academicmonitor.identity.config.AuthProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {
    public static final String ISSUER = "academic-monitor";
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;

    public AccessTokenService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(AcademicMonitorPrincipal principal) {
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(principal.userId().toString())
                .claim("institutionId", principal.institutionId().toString())
                .claim("systemRole", principal.systemRole().name())
                .claim("institutionRole", principal.institutionRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTtl()))
                .id(UUID.randomUUID().toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
