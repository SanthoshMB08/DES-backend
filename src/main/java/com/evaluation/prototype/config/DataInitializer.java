package com.evaluation.prototype.config;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.user.entity.Permission;
import com.evaluation.prototype.user.entity.Role;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.PermissionRepository;
import com.evaluation.prototype.user.repository.RoleRepository;
import com.evaluation.prototype.user.repository.UserRepository;
import com.evaluation.prototype.valuation.entity.Course;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationBundle;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.SubjectStatus;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.CourseRepository;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationBundleRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final String INVIGILATOR_ROLE = "ROLE_INVIGILATOR";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String SUPPORT_STAFF_ROLE = "ROLE_SUPPORT_STAFF";
    private static final String SCANNER_ROLE = "ROLE_SCANNER";
    private static final String VALUATOR_ROLE = "ROLE_VALUATOR"; // Evaluator
    private static final String REVIEWER_ROLE = "ROLE_REVIEWER";
    private static final String BOE_ROLE = "ROLE_BOE"; // Board Officer (BO)
    private static final String CHIEF_ROLE = "ROLE_CHIEF"; // Subject Chief
    private static final String RE_ROLE = "ROLE_RE"; // Register of Evaluation

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValuationBundleRepository valuationBundleRepository;
    private final ValuationScriptRepository valuationScriptRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ValuationBundleRepository valuationBundleRepository,
            ValuationScriptRepository valuationScriptRepository,
            SubjectRepository subjectRepository,
            CourseRepository courseRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.valuationBundleRepository = valuationBundleRepository;
        this.valuationScriptRepository = valuationScriptRepository;
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Permission> permissions = seedPermissions();
        Role adminRole = seedRole(ADMIN_ROLE, "Unrestricted system administrator", permissions);
        Role invigilatorRole = seedRole(INVIGILATOR_ROLE, "Exam hall invigilator",
                permissions.stream()
                        .filter(permission -> List.of("SCAN_HALL_TICKET", "SCAN_BOOKLET", "MARK_ABSENT")
                                .contains(permission.getName()))
                        .toList());

        Role supportStaffRole = seedRole(SUPPORT_STAFF_ROLE, "Digitizing support staff",
                permissions.stream()
                        .filter(permission -> List.of("UPLOAD_SCANNED_BOOKLET", "CONFIRM_SCANNED_UPLOAD", "COMPLETE_BUNDLE", "REPLACE_SCANNED_BOOKLET")
                                .contains(permission.getName()))
                        .toList());

        Role scannerRole = seedRole(SCANNER_ROLE, "Scans and uploads answer booklets", List.of());
        Role reRole = seedRole(RE_ROLE, "Register of Evaluation - creates Courses, appoints Board Officers", List.of());
        Role boeRole = seedRole(BOE_ROLE, "Board Officer - oversees every subject in a course, appoints Subject Chiefs", List.of());
        Role chiefRole = seedRole(CHIEF_ROLE, "Subject Chief - forms bundles, appoints the Reviewer for one subject", List.of());
        Role reviewerRole = seedRole(REVIEWER_ROLE, "Reviewer - appoints Evaluators, reviews and fixes marks", List.of());
        Role valuatorRole = seedRole(VALUATOR_ROLE, "Evaluator - marks answer scripts", List.of());

        userRepository.findByUsername("admin").orElseGet(() -> {
            User user = new User();
            user.setUsername("admin");
            user.setPasswordHash(passwordEncoder.encode("ChangeMe123"));
            user.setFullName("Default System Administrator");
            user.setForcePasswordChange(true);
            user.getRoles().add(adminRole);
            return userRepository.save(user);
        });

        userRepository.findByUsername("invigilator").orElseGet(() -> {
            User user = new User();
            user.setUsername("invigilator");
            user.setPasswordHash(passwordEncoder.encode("password"));
            user.setFullName("Default Invigilator");
            user.getRoles().add(invigilatorRole);
            return userRepository.save(user);
        });

        userRepository.findByUsername("support").orElseGet(() -> {
            User user = new User();
            user.setUsername("support");
            user.setPasswordHash(passwordEncoder.encode("staff123"));
            user.setFullName("Default Support Staff");
            user.getRoles().add(supportStaffRole);
            return userRepository.save(user);
        });

        User scanner = userRepository.findByUsername("scanner").orElseGet(() -> {
            User user = new User();
            user.setUsername("scanner");
            user.setPasswordHash(passwordEncoder.encode("scanner123"));
            user.setFullName("Scanning Desk");
            user.getRoles().add(scannerRole);
            return userRepository.save(user);
        });

        User re = userRepository.findByUsername("re").orElseGet(() -> {
            User user = new User();
            user.setUsername("re");
            user.setPasswordHash(passwordEncoder.encode("re123"));
            user.setFullName("Register of Evaluation");
            user.getRoles().add(reRole);
            return userRepository.save(user);
        });

        // ---- Demo chain of command: RE -> Course -> BO -> Subject -> Chief -> Reviewer -> Evaluator ----
        Course course = courseRepository.findByCourseNameIgnoreCaseAndSemesterAndSyllabusYearIgnoreCase(
                "BCA", "6", "NEP-2020").orElseGet(() -> {
            Course c = new Course();
            c.setCourseName("BCA");
            c.setSemester("6");
            c.setSyllabusYear("NEP-2020");
            c.setCreatedBy(re);
            return courseRepository.save(c);
        });
        if (course.getSyllabusYear() == null || course.getSyllabusYear().isBlank()
                || "LEGACY".equalsIgnoreCase(course.getSyllabusYear())) {
            course.setSyllabusYear("NEP-2020");
            courseRepository.save(course);
        }

        User bo = userRepository.findByUsername("bo").orElseGet(() -> {
            User user = new User();
            user.setUsername("bo");
            user.setPasswordHash(passwordEncoder.encode("bo123"));
            user.setFullName("Board Officer - BCA Sem 6");
            user.setManager(re);
            user.getRoles().add(boeRole);
            return userRepository.save(user);
        });
        if (course.getBo() == null) {
            course.setBo(bo);
            courseRepository.save(course);
        }

        Subject subject = subjectRepository.findByQpCode("ECD006").orElseGet(() -> {
            Subject s = new Subject();
            s.setSubjectCode("ECD006");
            s.setSubjectName("Data Structures");
            s.setCourse(course.getCourseName());
            s.setSemester(course.getSemester());
            s.setCourseRef(course);
            s.setQpCode("ECD006");
            s.setStatus(SubjectStatus.OPEN);
            s.setOpenedBy(re);
            return subjectRepository.save(s);
        });

        User chief = userRepository.findByUsername("chief").orElseGet(() -> {
            User user = new User();
            user.setUsername("chief");
            user.setPasswordHash(passwordEncoder.encode("chief123"));
            user.setFullName("Subject Chief - Data Structures");
            user.setManager(bo);
            user.setSubjectId(subject.getId());
            user.getRoles().add(chiefRole);
            return userRepository.save(user);
        });
        if (subject.getChief() == null) {
            subject.setChief(chief);
            subjectRepository.save(subject);
        }

        User reviewer = userRepository.findByUsername("reviewer").orElseGet(() -> {
            User user = new User();
            user.setUsername("reviewer");
            user.setPasswordHash(passwordEncoder.encode("reviewer123"));
            user.setFullName("Default Reviewer");
            user.setManager(chief);
            user.setSubjectId(subject.getId());
            user.getRoles().add(reviewerRole);
            return userRepository.save(user);
        });

        User evaluator = userRepository.findByUsername("valuator").orElseGet(() -> {
            User user = new User();
            user.setUsername("valuator");
            user.setPasswordHash(passwordEncoder.encode("valuator123"));
            user.setFullName("Default Evaluator");
            user.setManager(reviewer);
            user.setSubjectId(subject.getId());
            user.getRoles().add(valuatorRole);
            return userRepository.save(user);
        });

        seedDemoValuationData(scanner, subject);
    }

    /**
     * Seeds a couple of unbundled scanned scripts for the demo subject so the BO's
     * "form bundle" action has something to work with right after a fresh install.
     */
    private void seedDemoValuationData(User scanner, Subject subject) {
        if (!valuationScriptRepository.findByAnswerBookletId("AB-001231").isPresent()) {
            createDemoScript(scanner, subject, "AB-001231", "U18UZ22S012", "Ravi Shankar");
        }
        if (!valuationScriptRepository.findByAnswerBookletId("AB-001232").isPresent()) {
            createDemoScript(scanner, subject, "AB-001232", "U18UZ22S013", "Priya Menon");
        }
    }

    private void createDemoScript(User scanner, Subject subject, String answerBookletId, String candidateId, String candidateName) {
        ValuationScript script = new ValuationScript();
        script.setSubjectId(subject.getId());
        script.setAnswerBookletId(answerBookletId);
        script.setCandidateId(candidateId);
        script.setCandidateName(candidateName);
        script.setQpCode(subject.getQpCode());
        script.setSubject(subject.getSubjectName());
        script.setExamDate(LocalDate.of(2026, 4, 12));
        script.setCourse(subject.getCourse());
        script.setSemester(subject.getSemester());
        script.setCenterCode("CTR-0062");
        script.setScannedFileName(answerBookletId + ".pdf");
        script.setStatus(ValuationStatus.PENDING);
        script.setCreatedBy(scanner);
        valuationScriptRepository.save(script);
    }

    private List<Permission> seedPermissions() {
        return List.of(
                "CREATE_USER",
                "DELETE_USER",
                "VIEW_AUDIT",
                "MARK_ABSENT",
                "SCAN_HALL_TICKET",
                "SCAN_BOOKLET",
                "VIEW_REPORTS",
                "MANAGE_ROLES",
                "MANAGE_PERMISSIONS",
                "RESET_PASSWORD",
                "UNLOCK_USER").stream()
                .map(name -> permissionRepository.findByName(name).orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName(name);
                    permission.setDescription(name.replace('_', ' '));
                    return permissionRepository.save(permission);
                }))
                .toList();
    }

    private Role seedRole(String name, String description, List<Permission> permissions) {
        Role role = roleRepository.findByName(name).orElseGet(() -> {
            Role created = new Role();
            created.setName(name);
            return roleRepository.save(created);
        });
        role.setDescription(description);
        role.setEnabled(true);
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        return roleRepository.save(role);
    }
}
