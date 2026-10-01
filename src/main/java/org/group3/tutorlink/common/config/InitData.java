package org.group3.tutorlink.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.auth.repository.RoleRepository;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.StudentRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.repository.AdminRepository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Profile("dev")
@RequiredArgsConstructor
@Component
public class InitData implements CommandLineRunner {

    private final AppUtil appUtil;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubjectRepository subjectRepo;
    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("========== START INIT DATA ==========");

        initRoles();
        initStudents();
        initTutors();
        initSubjects();
        initAdmins();

        log.info("========== INIT DATA COMPLETED ==========");
    }

    private void initSubjects() {
        if (subjectRepository.count() > 0) {
            log.info("Subjects already exist. Skip.");
            return;
        }

        Subject math = Subject.builder()
                .id(appUtil.generateUUID())
                .name("Mathematics")
                .description("Mathematics subject")
                .build();

        Subject physics = Subject.builder()
                .id(appUtil.generateUUID())
                .name("Physics")
                .description("Physics subject")
                .build();

        Subject chemistry = Subject.builder()
                .id(appUtil.generateUUID())
                .name("Chemistry")
                .description("Chemistry subject")
                .build();

        subjectRepository.saveAll(List.of(math, physics, chemistry));

        log.info("✓ Initialized 3 subjects");
    }

    // =========================================================
    // ROLE
    // =========================================================

    private void initRoles() {

        if (roleRepository.count() > 0) {
            log.info("Roles already exist. Skip.");
            return;
        }

        Role studentRole = Role.builder()
                .id(appUtil.generateUUID())
                .name(RoleName.STUDENT.name())
                .description("Student role")
                .build();

        Role tutorRole = Role.builder()
                .id(appUtil.generateUUID())
                .name(RoleName.TUTOR.name())
                .description("Tutor role")
                .build();

        Role adminRole = Role.builder()
                .id(appUtil.generateUUID())
                .name(RoleName.ADMIN.name())
                .description("Administrator role")
                .build();

        roleRepository.saveAll(
                List.of(
                        studentRole,
                        tutorRole,
                        adminRole
                )
        );

        log.info("✓ Initialized 3 roles");
    }

    // =========================================================
    // STUDENT
    // =========================================================

    private void initStudents() {

        if (studentRepository.count() > 0) {
            log.info("Students already exist. Skip.");
            return;
        }

        Role studentRole = roleRepository.findByName(RoleName.STUDENT.name())
                .orElseThrow(() ->
                        new RuntimeException("ROLE_STUDENT not found"));

        String password = passwordEncoder.encode("Student@123");

        Student student1 = Student.builder()
                .id(appUtil.generateUUID())
                .fullname("Nguyen Van Student")
                .email("student@gmail.com")
                .password(password)
                .phone("0901234567")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2004, 1, 1))
                .address(Address.builder()
                        .street("123 Nguyen Trai")
                        .ward("Phuong 5")
                        .province("Ho Chi Minh City")
                        .build())
                .userStatus(UserStatus.ACTIVE)
                .role(studentRole)
                .grade("12")
                .learningGoal("Improve programming skills")
                .build();

        Student student2 = Student.builder()
                .id(appUtil.generateUUID())
                .fullname("Tran Thi Student")
                .email("student2@gmail.com")
                .password(password)
                .phone("0901234568")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2005, 5, 10))
                .address(Address.builder()
                        .street("123 Nguyen Trai")
                        .ward("Phuong 5")
                        .province("Ho Chi Minh City")
                        .build())
                .userStatus(UserStatus.ACTIVE)
                .role(studentRole)
                .grade("11")
                .learningGoal("Improve English")
                .build();

        studentRepository.saveAll(
                List.of(student1, student2)
        );

        log.info("✓ Initialized 2 students");
        log.info("  Student 1: student@gmail.com / Student@123");
        log.info("  Student 2: student2@gmail.com / Student@123");
    }

    //Admin

    private void initAdmins() {

        if (adminRepository.count() > 0) {
            log.info("Admins already exist. Skip.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN.name())
                .orElseThrow(() ->
                        new RuntimeException("ROLE_ADMIN not found"));

        String password = passwordEncoder.encode("Admin@123");

        Admin admin = Admin.builder()
                .id(appUtil.generateUUID())
                .fullname("System Admin")
                .email("admin@gmail.com")
                .password(password)
                .phone("0901234571")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .address(Address.builder()
                        .street("123 Nguyen Trai")
                        .ward("Phuong 5")
                        .province("Ho Chi Minh City")
                        .build())
                .userStatus(UserStatus.ACTIVE)
                .role(adminRole)
                .department("Administration")
                .build();

        adminRepository.save(admin);

        log.info("✓ Initialized 1 admin");
        log.info("  Admin: admin@gmail.com / Admin@123");
    }
    // =========================================================
    // TUTOR
    // =========================================================

    private void initTutors() {

        if (tutorRepository.count() > 0) {
            log.info("Tutors already exist. Skip.");
            return;
        }

        Role tutorRole = roleRepository.findByName(RoleName.TUTOR.name())
                .orElseThrow(() ->
                        new RuntimeException("ROLE_TUTOR not found"));

        List<Subject> subjects = subjectRepository.findAll();

        if (subjects.isEmpty()) {
            log.warn("⚠ No Subject found. Skip initializing tutors.");
            return;
        }

        Subject subject = subjects.get(0);

        String password = passwordEncoder.encode("Tutor@123");

        Tutor tutor1 = Tutor.builder()
                .id(appUtil.generateUUID())
                .fullname("Nguyen Van Tutor")
                .email("tutor@gmail.com")
                .password(password)
                .phone("0901234569")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1998, 3, 15))
                .address(Address.builder()
                        .street("123 Nguyen Trai")
                        .ward("Phuong 5")
                        .province("Ho Chi Minh City")
                        .build())
                .userStatus(UserStatus.ACTIVE)
                .role(tutorRole)
                .experienceYears(5)
                .education("Bachelor of Information Technology")
                .subject(subject)
                .averageRating(0.0)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        Tutor tutor2 = Tutor.builder()
                .id(appUtil.generateUUID())
                .fullname("Tran Thi Tutor")
                .email("tutor2@gmail.com")
                .password(password)
                .phone("0901234570")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(1997, 8, 20))
                .address(Address.builder()
                        .street("123 Nguyen Trai")
                        .ward("Phuong 5")
                        .province("Ho Chi Minh City")
                        .build())
                .userStatus(UserStatus.ACTIVE)
                .role(tutorRole)
                .experienceYears(7)
                .education("Master of Information Technology")
                .subject(subject)
                .averageRating(0.0)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        tutorRepository.saveAll(
                List.of(tutor1, tutor2)
        );

        log.info("✓ Initialized 2 tutors");
        log.info("  Tutor 1: tutor@gmail.com / Tutor@123");
        log.info("  Tutor 2: tutor2@gmail.com / Tutor@123");
    }
}