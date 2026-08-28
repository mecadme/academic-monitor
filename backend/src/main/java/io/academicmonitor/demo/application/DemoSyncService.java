package io.academicmonitor.demo.application;

import io.academicmonitor.academic.application.AcademicSyncResult;
import io.academicmonitor.academic.application.AcademicSyncService;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.demo.infrastructure.platform.FakeAcademicPlatformAdapterFactory;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import org.springframework.stereotype.Service;

@Service
public class DemoSyncService {

    private static final String PLATFORM = "DEMO";

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final AcademicSyncService academicSyncService;
    private final FakeAcademicPlatformAdapterFactory platformAdapterFactory;

    public DemoSyncService(
            UserRepository userRepository,
            InstitutionRepository institutionRepository,
            InstitutionMembershipRepository membershipRepository,
            AcademicSyncService academicSyncService,
            FakeAcademicPlatformAdapterFactory platformAdapterFactory) {

        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.academicSyncService = academicSyncService;
        this.platformAdapterFactory = platformAdapterFactory;
    }

    public DemoSyncResult sync(DemoScenario scenario) {

        User teacher = userRepository
                .findByEmail("demo.teacher@academicmonitor.local")
                .orElseGet(() -> userRepository.save(new User("demo.teacher@academicmonitor.local")));

        Institution institution = resolveInstitution(teacher);

        AcademicPlatformPort platform = platformAdapterFactory.create(scenario);

        AcademicSyncResult sync =
                academicSyncService.synchronize(institution.getId(), teacher.getId(), PLATFORM, platform);

        return new DemoSyncResult(
                institution.getId(),
                teacher.getId(),
                sync.courseId(),
                sync.courseName(),
                scenario,
                sync.students(),
                sync.gradesProcessed(),
                sync.openAlerts(),
                sync.warnings(),
                sync.critical());
    }

    private Institution resolveInstitution(User teacher) {

        return membershipRepository.findByUserId(teacher.getId()).stream()
                .filter(InstitutionMembership::isActive)
                .findFirst()
                .flatMap(membership -> institutionRepository.findById(membership.getInstitutionId()))
                .orElseGet(() -> {
                    Institution institution =
                            institutionRepository.save(new Institution("Unidad Educativa Demo", "America/Guayaquil"));

                    membershipRepository.save(
                            new InstitutionMembership(teacher.getId(), institution.getId(), InstitutionRole.TEACHER));

                    return institution;
                });
    }
}
