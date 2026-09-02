package io.academicmonitor.context.application;

import io.academicmonitor.context.config.AcademicContextProperties;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicContextBootstrapService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final AcademicContextProperties properties;

    public AcademicContextBootstrapService(
            UserRepository userRepository,
            InstitutionRepository institutionRepository,
            InstitutionMembershipRepository membershipRepository,
            AcademicContextProperties properties) {

        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.properties = properties;
    }

    @Transactional
    public AcademicContextResult bootstrap() {

        User bootstrapUser = new User(properties.userEmail());

        User user = userRepository
                .findByEmail(bootstrapUser.getEmail())
                .orElseGet(() -> userRepository.save(bootstrapUser));

        List<InstitutionMembership> activeMemberships = membershipRepository.findByUserId(user.getId()).stream()
                .filter(InstitutionMembership::isActive)
                .toList();

        if (activeMemberships.size() > 1) {
            throw new IllegalStateException("Bootstrap user has multiple active institution memberships");
        }

        if (activeMemberships.size() == 1) {
            Institution institution = resolveInstitution(activeMemberships.get(0));

            return new AcademicContextResult(institution.getId(), user.getId());
        }

        Institution institution =
                institutionRepository.save(new Institution(properties.institutionName(), properties.timezone()));

        membershipRepository.save(
                new InstitutionMembership(user.getId(), institution.getId(), InstitutionRole.TEACHER));

        return new AcademicContextResult(institution.getId(), user.getId());
    }

    private Institution resolveInstitution(InstitutionMembership membership) {

        Institution institution = institutionRepository
                .findById(membership.getInstitutionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Bootstrap user's active membership references a missing institution"));

        if (!institution.isActive()) {
            throw new IllegalStateException("Bootstrap user's active membership references an inactive institution");
        }

        return institution;
    }
}
