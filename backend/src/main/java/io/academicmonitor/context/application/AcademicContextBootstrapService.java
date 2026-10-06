package io.academicmonitor.context.application;

import io.academicmonitor.context.config.AcademicContextProperties;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
public class AcademicContextBootstrapService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final AcademicContextProperties properties;
    private final PasswordEncoder passwordEncoder;

    public AcademicContextBootstrapService(
            UserRepository userRepository,
            InstitutionRepository institutionRepository,
            InstitutionMembershipRepository membershipRepository,
            AcademicContextProperties properties,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void bootstrap() {

        User bootstrapUser = new User(properties.userEmail());

        User user = userRepository
                .findByEmail(bootstrapUser.getEmail())
                .orElseGet(() -> userRepository.save(bootstrapUser));

        if (!user.isActive()) {
            return;
        }

        if (user.getPasswordHash() == null
                && properties.password() != null
                && !properties.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(properties.password()));
            userRepository.save(user);
        }

        // Existing memberships (including disabled/multiple memberships) are never rewritten.
        // Institution selection belongs to login, not development initialization.
        if (!membershipRepository.findByUserId(user.getId()).isEmpty()) {
            return;
        }

        Institution institution =
                institutionRepository.save(new Institution(properties.institutionName(), properties.timezone()));

        membershipRepository.save(
                new InstitutionMembership(user.getId(), institution.getId(), InstitutionRole.TEACHER));
    }
}
