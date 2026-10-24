package org.group3.tutorlink.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.auth.repository.RoleRepository;
import org.group3.tutorlink.features.notification.entity.Notification;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.repository.NotificationRepository;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.StudentRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.repository.AdminRepository;

import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.repository.PostRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    private final ApplicationRepository applicationRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("========== START INIT DATA ==========");

        initRoles();
        initStudents();
        initSubjects();
        initTutors();
        initAdmins();
        initNotifications();
        initStatisticsData();
        log.info("========== INIT DATA COMPLETED ==========");
    }
    private void initNotifications() {
        if (notificationRepository.count() > 0) {
            log.info("Notifications already exist. Skip.");
            return;
        }

        var student = userRepository.findByEmail("student@gmail.com")
                .orElseThrow(() -> new RuntimeException("student@gmail.com not found"));

        var tutor1 = userRepository.findByEmail("tutor@gmail.com")
                .orElseThrow(() -> new RuntimeException("tutor@gmail.com not found"));

        var tutor2 = userRepository.findByEmail("tutor2@gmail.com")
                .orElseThrow(() -> new RuntimeException("tutor2@gmail.com not found"));

        List<Notification> notifications = new ArrayList<>( );

        List<User> users = List.of(student, tutor1, tutor2);

        for (User user : users) {
            notifications.add(Notification.builder()
                    .id(appUtil.generateUUID())
                    .title("Chào mừng bạn đến với TutorLink")
                    .content("Tài khoản của bạn đã được tạo thành công.")
                    .notificationType(NotificationType.SYSTEM)
                    .isRead(false)
                    .recipient(user)
                    .build());

            notifications.add(Notification.builder()
                    .id(appUtil.generateUUID())
                    .title("Cập nhật hồ sơ")
                    .content("Vui lòng kiểm tra và cập nhật thông tin hồ sơ của bạn.")
                    .notificationType(NotificationType.APPLICATION)
                    .isRead(false)
                    .recipient(user)
                    .build());

            notifications.add(Notification.builder()
                    .id(appUtil.generateUUID())
                    .title("Thông báo mới")
                    .content("Bạn có thông báo mới từ TutorLink.")
                    .notificationType(NotificationType.SYSTEM)
                    .isRead(true)
                    .recipient(user)
                    .build());
        }

        notificationRepository.saveAll(notifications);

        log.info("Initialized {} notifications for {} users",
                notifications.size(), users.size());
    }
    // =========================================================
    // DỮ LIỆU TEST CHO /v1/statistics/*  (post -> application -> wallet -> transaction)
    // =========================================================

    private void initStatisticsData() {

        if (applicationRepository.count() > 0) {
            log.info("Applications already exist. Skip statistics data.");
            return;
        }

        Student student1 = studentRepository.findAll().stream()
                .filter(s -> "student@gmail.com".equals(s.getEmail())).findFirst()
                .orElseThrow(() -> new RuntimeException("student@gmail.com not found"));
        Student student2 = studentRepository.findAll().stream()
                .filter(s -> "student2@gmail.com".equals(s.getEmail())).findFirst()
                .orElseThrow(() -> new RuntimeException("student2@gmail.com not found"));
        Tutor tutor1 = tutorRepository.findAll().stream()
                .filter(t -> "tutor@gmail.com".equals(t.getEmail())).findFirst()
                .orElseThrow(() -> new RuntimeException("tutor@gmail.com not found"));
        Tutor tutor2 = tutorRepository.findAll().stream()
                .filter(t -> "tutor2@gmail.com".equals(t.getEmail())).findFirst()
                .orElseThrow(() -> new RuntimeException("tutor2@gmail.com not found"));

        List<Subject> subjects = subjectRepository.findAll();
        Subject math = subjects.get(0);
        Subject physics = subjects.size() > 1 ? subjects.get(1) : math;

        Post post1 = buildPost(student1, math, "Tìm gia sư Toán lớp 12", new BigDecimal("2000000"));
        Post post2 = buildPost(student1, physics, "Tìm gia sư Lý ôn thi đại học", new BigDecimal("3000000"));
        Post post3 = buildPost(student2, math, "Tìm gia sư Toán lớp 11", new BigDecimal("1500000"));
        Post post4 = buildPost(student2, physics, "Tìm gia sư Lý lớp 10", new BigDecimal("1000000"));
        postRepository.saveAll(List.of(post1, post2, post3, post4));

        // tutor1: 3 ACCEPTED | tutor2: 1 ACCEPTED + 1 PENDING + 1 REJECTED
        Application app1 = buildApplication(post1, student1, tutor1, ApplicationStatus.ACCEPTED);
        Application app2 = buildApplication(post2, student1, tutor1, ApplicationStatus.ACCEPTED);
        Application app3 = buildApplication(post3, student2, tutor1, ApplicationStatus.ACCEPTED);
        Application app4 = buildApplication(post4, student2, tutor2, ApplicationStatus.ACCEPTED);
        Application app5 = buildApplication(post1, student1, tutor2, ApplicationStatus.PENDING);
        Application app6 = buildApplication(post2, student1, tutor2, ApplicationStatus.REJECTED);
        applicationRepository.saveAll(List.of(app1, app2, app3, app4, app5, app6));

        // Ví + nạp tiền (TOP_UP SUCCESS)
        BigDecimal topUp1 = new BigDecimal("2000000.00");
        BigDecimal topUp2 = new BigDecimal("1000000.00");

        // Phí hoa hồng = 20% maxBudget
        BigDecimal fee1 = new BigDecimal("400000.00");  // post1
        BigDecimal fee2 = new BigDecimal("600000.00");  // post2
        BigDecimal fee4 = new BigDecimal("200000.00");  // post4

        // tutor1 trả phí app1, app2 (SUCCESS); app3 chưa trả (không có transaction)
        walletRepository.save(Wallet.builder().id(appUtil.generateUUID())
                .tutor(tutor1).balance(topUp1.subtract(fee1).subtract(fee2)).build());
        // tutor2 trả phí app4 (SUCCESS)
        walletRepository.save(Wallet.builder().id(appUtil.generateUUID())
                .tutor(tutor2).balance(topUp2.subtract(fee4)).build());

        transactionRepository.saveAll(List.of(
                buildTransaction(tutor1, null, TransactionType.TOP_UP, topUp1, TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER),
                buildTransaction(tutor2, null, TransactionType.TOP_UP, topUp2, TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER),
                buildTransaction(tutor1, app1, TransactionType.COMMISSION, fee1, TransactionStatus.SUCCESS, PaymentMethod.WALLET),
                buildTransaction(tutor1, app2, TransactionType.COMMISSION, fee2, TransactionStatus.SUCCESS, PaymentMethod.WALLET),
                buildTransaction(tutor2, app4, TransactionType.COMMISSION, fee4, TransactionStatus.SUCCESS, PaymentMethod.WALLET)
        ));

        log.info("✓ Initialized statistics test data: 4 posts, 6 applications (4 ACCEPTED), 2 wallets, 5 transactions");
        log.info("  Kỳ vọng: accepted = 4 | top-tutors: Nguyen Van Tutor = 3, Tran Thi Tutor = 1 | revenue = 1,200,000 (3 giao dịch COMMISSION SUCCESS)");
    }

    private Post buildPost(Student author, Subject subject, String title, BigDecimal maxBudget) {
        return Post.builder()
                .id(appUtil.generateUUID())
                .title(title)
                .content(title + " - học 2 buổi/tuần")
                .type(PostType.FIND_TUTOR)
                .status(PostStatus.PUBLISHED)
                .author(author)
                .subject(subject)
                .educationLevel(EducationLevel.HIGH_SCHOOL)
                .teachingMode(TeachingMode.BOTH)
                .address(Address.builder()
                        .street("123 Nguyen Trai")
                        .ward("Phuong 5")
                        .province("Ho Chi Minh City")
                        .build())
                .minBudget(maxBudget.subtract(new BigDecimal("500000")))
                .maxBudget(maxBudget)
                .sessionsPerWeek(2)
                .durationMinutes(90)
                .deadline(Instant.now().plusSeconds(30L * 24 * 3600))
                .build();
    }

    private Application buildApplication(Post post, Student student, Tutor tutor, ApplicationStatus status) {
        return Application.builder()
                .id(appUtil.generateUUID())
                .applicationStatus(status)
                .message("Tôi muốn nhận lớp này")
                .appliedAt(Instant.now())
                .post(post)
                .student(student)
                .tutor(tutor)
                .build();
    }

    private Transaction buildTransaction(Tutor tutor, Application application, TransactionType type,
                                         BigDecimal amount, TransactionStatus status, PaymentMethod method) {
        return Transaction.builder()
                .id(appUtil.generateUUID())
                .type(type)
                .amount(amount)
                .status(status)
                .paymentMethod(method)
                .transactionDate(Instant.now())
                .tutor(tutor)
                .application(application)
                .build();
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
                .name(RoleName.STUDENT)
                .description("Student role")
                .build();

        Role tutorRole = Role.builder()
                .id(appUtil.generateUUID())
                .name(RoleName.TUTOR)
                .description("Tutor role")
                .build();

        Role adminRole = Role.builder()
                .id(appUtil.generateUUID())
                .name(RoleName.ADMIN)
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

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() ->
                        new RuntimeException("ROLE_STUDENT not found"));

        String password = passwordEncoder.encode("Student@123");

        Student student1 = Student.builder()
                .id(appUtil.generateUUID())
                .fullName("Nguyen Van Student")
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
                .fullName("Tran Thi Student")
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

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() ->
                        new RuntimeException("ROLE_ADMIN not found"));

        String password = passwordEncoder.encode("Admin@123");

        Admin admin = Admin.builder()
                .id(appUtil.generateUUID())
                .fullName("System Admin")
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

        Role tutorRole = roleRepository.findByName(RoleName.TUTOR)
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
                .fullName("Nguyen Van Tutor")
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
                .verificationStatus(VerificationStatus.APPROVED)
                .build();

        Tutor tutor2 = Tutor.builder()
                .id(appUtil.generateUUID())
                .fullName("Tran Thi Tutor")
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