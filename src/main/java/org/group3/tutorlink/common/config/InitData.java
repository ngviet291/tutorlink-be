package org.group3.tutorlink.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.auth.repository.RoleRepository;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.chat.entity.Message;
import org.group3.tutorlink.features.chat.repository.ChatChannelRepository;
import org.group3.tutorlink.features.chat.repository.MessageRepository;
import org.group3.tutorlink.features.favorite.entity.Favorite;
import org.group3.tutorlink.features.favorite.repository.FavoriteRepository;
import org.group3.tutorlink.features.notification.entity.Notification;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.repository.NotificationRepository;
import org.group3.tutorlink.features.payment.entity.MatchingFeeConfig;
import org.group3.tutorlink.features.payment.entity.RevenueReport;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.payment.repository.MatchingFeeConfigRepository;
import org.group3.tutorlink.features.payment.repository.RevenueReportRepository;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.*;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.review.entity.Review;
import org.group3.tutorlink.features.review.entity.ReviewComplaint;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;
import org.group3.tutorlink.features.review.repository.ReviewComplaintRepository;
import org.group3.tutorlink.features.review.repository.ReviewRepository;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.entity.*;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.AdminRepository;
import org.group3.tutorlink.features.user.repository.StudentRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Dữ liệu mẫu cho môi trường dev (profile "dev"), sinh bằng Datafaker (locale vi, seed cố định
 * nên mỗi lần chạy ra cùng một bộ dữ liệu).
 * <p>
 * Email/mật khẩu cố định (để đăng nhập demo), phần còn lại (tên, SĐT, địa chỉ, ngày sinh,
 * tiểu sử, nội dung bài đăng, tin nhắn, đánh giá...) do Faker sinh:
 * ADMIN   : admin@gmail.com                          / Admin@123
 * STUDENT : student@gmail.com ... student7@gmail.com / Student@123
 * TUTOR   : tutor@gmail.com, tutor2..tutor11@gmail.com / Tutor@123
 * <p>
 * Dependency: net.datafaker:datafaker (vd. 2.4.2).
 * <p>
 * Phí nhận lớp = maxBudget * 20% (khớp PaymentServiceImpl#calculateCommission).
 */
@Slf4j
@Profile("dev")
@RequiredArgsConstructor
@Component
public class InitData implements CommandLineRunner {

    private static final long SEED = 42L;
    private static final BigDecimal FEE_RATE = new BigDecimal("0.20");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final String HCM = "Ho Chi Minh City";
    private static final String HN = "Ha Noi";
    private static final String DN = "Da Nang";
    private static final String CT = "Can Tho";

    private static final String S_MATH = "Toán";
    private static final String S_LIT = "Ngữ văn";
    private static final String S_ENG = "Tiếng Anh";
    private static final String S_PHY = "Vật lý";
    private static final String S_CHE = "Hóa học";
    private static final String S_BIO = "Sinh học";
    private static final String S_IT = "Tin học";
    private static final String S_ART = "Âm nhạc / Nghệ thuật";

    private final Faker faker = new Faker(new Locale("vi"), new Random(SEED));

    private final AppUtil appUtil;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TutorRepository tutorRepository;
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    private final ApplicationRepository applicationRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final MatchingFeeConfigRepository matchingFeeConfigRepository;
    private final RevenueReportRepository revenueReportRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewComplaintRepository reviewComplaintRepository;
    private final FavoriteRepository favoriteRepository;
    private final ChatChannelRepository chatChannelRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;

    // ---- trạng thái dùng chung giữa các bước (chỉ sống trong 1 lần chạy) ----
    private final Map<String, Post> posts = new LinkedHashMap<>();
    private final Map<String, Application> apps = new LinkedHashMap<>();
    private final Map<String, Review> reviews = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<UUID, BigDecimal> ledger = new LinkedHashMap<>();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== START INIT DATA (Datafaker) ==========");

        initRoles();
        initSubjects();
        initStudents();
        initTutors();
        initAdmins();
        initFeeConfigs();
        initMarketplaceData();      // posts -> applications -> wallets -> transactions
        initReviews();
        initReviewComplaints();
        initFavorites();
        initChats();
        initNotifications();
        initRevenueReports();

        log.info("========== INIT DATA COMPLETED ==========");
    }

    private void initTutorSchedule(Tutor tutor) {
        List<DayOfWeek> days = new ArrayList<>(List.of(DayOfWeek.values()));
        Collections.shuffle(days);

        List<AvailableSlot> slots = days.subList(0, 3 + ThreadLocalRandom.current().nextInt(3))
                .stream()
                .map(day -> {
                    int startHour = ThreadLocalRandom.current().nextInt(7, 17);
                    return AvailableSlot.builder()
                            .dayOfWeek(day)
                            .startTime(LocalDate.of(2026, 1, 5)
                                    .with(TemporalAdjusters.nextOrSame(day))
                                    .atTime(startHour, 0)
                                    .toInstant(ZoneOffset.UTC))
                            .endTime(LocalDate.of(2026, 1, 5)
                                    .with(TemporalAdjusters.nextOrSame(day))
                                    .atTime(startHour + 2, 0)
                                    .toInstant(ZoneOffset.UTC))
                            .build();
                })
                .toList();

        List<String> areas = List.of(
                "Quận 1", "Quận 3", "Quận 7", "Bình Thạnh",
                "Gò Vấp", "Thủ Đức", "Tân Bình", "Phú Nhuận"
        );

        List<String> shuffledAreas = new ArrayList<>(areas);
        Collections.shuffle(shuffledAreas);

        tutor.setAvailableSlots(slots);
        tutor.setTeachingAreas(shuffledAreas.subList(
                0, ThreadLocalRandom.current().nextInt(2, 5)
        ));
    }
    // =========================================================
    // ROLE
    // =========================================================
    private void initRoles() {
        if (roleRepository.count() > 0) {
            log.info("Roles already exist. Skip.");
            return;
        }
        roleRepository.saveAll(List.of(
                Role.builder().id(appUtil.generateUUID()).name(RoleName.STUDENT).description("Student role").build(),
                Role.builder().id(appUtil.generateUUID()).name(RoleName.TUTOR).description("Tutor role").build(),
                Role.builder().id(appUtil.generateUUID()).name(RoleName.ADMIN).description("Administrator role").build()
        ));
        log.info("✓ Initialized 3 roles");
    }

    // =========================================================
    // SUBJECT (danh mục cố định, không dùng faker)
    // =========================================================
    private void initSubjects() {
        if (subjectRepository.count() > 0) {
            log.info("Subjects already exist. Skip.");
            return;
        }
        subjectRepository.saveAll(List.of(
                subject(S_MATH, "Đại số, hình học, giải tích; ôn thi vào 10 và THPT quốc gia", "Khoa học tự nhiên"),
                subject(S_LIT, "Đọc hiểu, nghị luận xã hội, nghị luận văn học", "Khoa học xã hội"),
                subject(S_ENG, "Giao tiếp, ngữ pháp, IELTS, TOEIC", "Ngoại ngữ"),
                subject(S_PHY, "Cơ, nhiệt, điện, quang, vật lý hiện đại", "Khoa học tự nhiên"),
                subject(S_CHE, "Hóa vô cơ, hữu cơ, bài tập nâng cao", "Khoa học tự nhiên"),
                subject(S_BIO, "Di truyền, sinh thái, sinh học tế bào", "Khoa học tự nhiên"),
                subject(S_IT, "Lập trình cơ bản, Python, C++, Tin học văn phòng", "Công nghệ"),
                subject(S_ART, "Piano, guitar, thanh nhạc, mỹ thuật cơ bản", "Năng khiếu")
        ));
        log.info("✓ Initialized 8 subjects");
    }

    private Subject subject(String name, String description, String category) {
        return Subject.builder().id(appUtil.generateUUID()).name(name)
                .description(description).category(category).build();
    }

    // =========================================================
    // STUDENT
    // =========================================================
    private void initStudents() {
        if (studentRepository.count() > 0) {
            log.info("Students already exist. Skip.");
            return;
        }
        Role role = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new RuntimeException("ROLE_STUDENT not found"));
        String pw = passwordEncoder.encode("Student@123");

        studentRepository.saveAll(List.of(
                student(role, pw, "student@gmail.com", Gender.MALE, 17, HCM, UserStatus.ACTIVE, "12"),
                student(role, pw, "student2@gmail.com", Gender.FEMALE, 16, HCM, UserStatus.ACTIVE, "11"),
                student(role, pw, "student3@gmail.com", Gender.MALE, 15, HN, UserStatus.ACTIVE, "10"),
                student(role, pw, "student4@gmail.com", Gender.FEMALE, 14, DN, UserStatus.ACTIVE, "9"),
                student(role, pw, "student5@gmail.com", Gender.MALE, 11, HCM, UserStatus.ACTIVE, "6"),
                student(role, pw, "student6@gmail.com", Gender.FEMALE, 21, CT, UserStatus.BANNED, "Đại học năm 2"),
                student(role, pw, "student7@gmail.com", Gender.MALE, 17, HCM, UserStatus.INACTIVE, "12")
        ));
        log.info("✓ Initialized 7 students (5 ACTIVE, 1 BANNED, 1 INACTIVE) — Student@123");
    }

    private Student student(Role role, String pw, String email, Gender gender, int age, String province,
                            UserStatus status, String grade) {
        return Student.builder()
                .id(appUtil.generateUUID()).fullName(fullName()).email(email).password(pw).phone(phone())
                .avatarUrl(avatar()).gender(gender).dateOfBirth(dob(age)).address(address(province))
                .userStatus(status).role(role).grade(grade).learningGoal(sentence(14))
                .build();
    }

    // =========================================================
    // TUTOR (kèm chứng chỉ)
    // =========================================================
    private void initTutors() {
        if (tutorRepository.count() > 0) {
            log.info("Tutors already exist. Skip.");
            return;
        }

        Role role = roleRepository.findByName(RoleName.TUTOR)
                .orElseThrow(() -> new RuntimeException("ROLE_TUTOR not found"));

        Map<String, Subject> sub = subjectByName();

        if (sub.isEmpty()) {
            log.warn("⚠ No Subject found. Skip initializing tutors.");
            return;
        }

        String pw = passwordEncoder.encode("Tutor@123");
        List<Tutor> list = new ArrayList<>();
        VerificationStatus ok = VerificationStatus.APPROVED;
        String okNote = "Hồ sơ hợp lệ";

        Tutor t1 = tutor(
                role, pw, "tutor@gmail.com", Gender.MALE, 28, HCM, 5, 150000,
                "Cử nhân CNTT - ĐH Bách Khoa TP.HCM",
                sub.get(S_MATH), 4.8, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.HIGH_SCHOOL, EducationLevel.UNIVERSITY),
                TeachingMode.BOTH
        );
        cert(t1, "Chứng chỉ nghiệp vụ sư phạm", "ĐH Sư phạm TP.HCM",
                LocalDate.of(2021, 6, 1), null, true);
        cert(t1, "Giải Ba Olympic Toán sinh viên", "Hội Toán học Việt Nam",
                LocalDate.of(2019, 5, 20), null, true);
        list.add(t1);

        Tutor t2 = tutor(
                role, pw, "tutor2@gmail.com", Gender.FEMALE, 29, HCM, 7, 200000,
                "Thạc sĩ CNTT - ĐH Khoa học Tự nhiên",
                sub.get(S_PHY), 0.0, VerificationStatus.PENDING, null, UserStatus.ACTIVE,
                Set.of(EducationLevel.UNIVERSITY),
                TeachingMode.ONLINE
        );
        cert(t2, "Bằng thạc sĩ", "ĐH Khoa học Tự nhiên",
                LocalDate.of(2022, 12, 1), null, false);
        list.add(t2);

        Tutor t3 = tutor(
                role, pw, "tutor3@gmail.com", Gender.MALE, 31, HN, 3, 80000,
                "Cử nhân Toán ứng dụng - ĐH Bách Khoa Hà Nội",
                sub.get(S_MATH), 4.2, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.PRIMARY, EducationLevel.SECONDARY,
                        EducationLevel.HIGH_SCHOOL),
                TeachingMode.OFFLINE
        );
        cert(t3, "Chứng chỉ nghiệp vụ sư phạm", "ĐH Sư phạm Hà Nội",
                LocalDate.of(2020, 8, 15), null, true);
        list.add(t3);

        Tutor t4 = tutor(
                role, pw, "tutor4@gmail.com", Gender.FEMALE, 30, DN, 4, 120000,
                "Cử nhân Hóa học - ĐH Bách Khoa Đà Nẵng",
                sub.get(S_CHE), 4.5, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.SECONDARY, EducationLevel.HIGH_SCHOOL),
                TeachingMode.BOTH
        );
        cert(t4, "Bằng cử nhân Hóa học loại Giỏi", "ĐH Bách Khoa Đà Nẵng",
                LocalDate.of(2018, 7, 10), null, true);
        list.add(t4);

        Tutor t5 = tutor(
                role, pw, "tutor5@gmail.com", Gender.MALE, 33, HCM, 8, 250000,
                "Thạc sĩ Vật lý - ĐH Khoa học Tự nhiên TP.HCM",
                sub.get(S_PHY), 4.9, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.HIGH_SCHOOL, EducationLevel.UNIVERSITY),
                TeachingMode.ONLINE
        );
        cert(t5, "Bằng thạc sĩ Vật lý", "ĐH Khoa học Tự nhiên TP.HCM",
                LocalDate.of(2017, 11, 20), null, true);
        cert(t5, "Chứng nhận bồi dưỡng học sinh giỏi", "Sở GD&ĐT TP.HCM",
                LocalDate.of(2020, 3, 5), null, true);
        list.add(t5);

        Tutor t6 = tutor(
                role, pw, "tutor6@gmail.com", Gender.FEMALE, 22, CT, 1, 60000,
                "Cử nhân Toán (đang học) - ĐH Cần Thơ",
                sub.get(S_MATH), 3.9, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.PRIMARY, EducationLevel.SECONDARY),
                TeachingMode.OFFLINE
        );
        cert(t6, "Giấy xác nhận sinh viên", "ĐH Cần Thơ",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30), true);
        list.add(t6);

        Tutor t7 = tutor(
                role, pw, "tutor7@gmail.com", Gender.MALE, 34, HCM, 6, 180000,
                "Cử nhân Ngôn ngữ Anh - ĐH Ngoại thương",
                sub.get(S_ENG), 4.7, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.SECONDARY, EducationLevel.HIGH_SCHOOL,
                        EducationLevel.UNIVERSITY),
                TeachingMode.BOTH
        );
        cert(t7, "IELTS Academic 8.0", "British Council",
                LocalDate.of(2024, 3, 12), LocalDate.of(2026, 3, 12), true);
        cert(t7, "TESOL Certificate", "Arizona State University",
                LocalDate.of(2022, 9, 1), null, true);
        list.add(t7);

        Tutor t8 = tutor(
                role, pw, "tutor8@gmail.com", Gender.FEMALE, 32, HN, 5, 130000,
                "Cử nhân Sư phạm Ngữ văn - ĐH Sư phạm Hà Nội",
                sub.get(S_LIT), 4.6, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.PRIMARY, EducationLevel.SECONDARY,
                        EducationLevel.HIGH_SCHOOL),
                TeachingMode.OFFLINE
        );
        cert(t8, "Bằng cử nhân Sư phạm Ngữ văn", "ĐH Sư phạm Hà Nội",
                LocalDate.of(2016, 6, 25), null, true);
        list.add(t8);

        Tutor t9 = tutor(
                role, pw, "tutor9@gmail.com", Gender.MALE, 30, HCM, 4, 220000,
                "Kỹ sư Công nghệ phần mềm - ĐH Công nghệ Thông tin",
                sub.get(S_IT), 4.4, ok, okNote, UserStatus.ACTIVE,
                Set.of(EducationLevel.HIGH_SCHOOL, EducationLevel.UNIVERSITY),
                TeachingMode.ONLINE
        );
        cert(t9, "AWS Certified Cloud Practitioner", "Amazon Web Services",
                LocalDate.of(2023, 4, 2), LocalDate.of(2026, 4, 2), true);
        cert(t9, "Giải Nhì Olympic Tin học sinh viên", "Hội Tin học Việt Nam",
                LocalDate.of(2018, 11, 18), null, true);
        list.add(t9);

        Tutor t10 = tutor(
                role, pw, "tutor10@gmail.com", Gender.FEMALE, 24, HCM, 2, 110000,
                "Sinh viên Y đa khoa - ĐH Y Dược TP.HCM",
                sub.get(S_BIO), 0.0, VerificationStatus.PENDING, null, UserStatus.ACTIVE,
                Set.of(EducationLevel.SECONDARY, EducationLevel.HIGH_SCHOOL),
                TeachingMode.BOTH
        );
        cert(t10, "Giấy xác nhận sinh viên Y khoa", "ĐH Y Dược TP.HCM",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30), false);
        list.add(t10);

        Tutor t11 = tutor(
                role, pw, "tutor11@gmail.com", Gender.MALE, 35, DN, 3, 140000,
                "Cử nhân Sư phạm Âm nhạc - ĐH Sư phạm Đà Nẵng",
                sub.get(S_ART), 0.0, VerificationStatus.REJECTED,
                "Ảnh chụp bằng cấp bị mờ, không đọc được thông tin. Vui lòng tải lại bản rõ nét và bổ sung chứng chỉ giảng dạy.",
                UserStatus.ACTIVE,
                Set.of(EducationLevel.PRIMARY, EducationLevel.SECONDARY),
                TeachingMode.OFFLINE
        );
        cert(t11, "Bằng cử nhân Sư phạm Âm nhạc (ảnh mờ)", "ĐH Sư phạm Đà Nẵng",
                LocalDate.of(2015, 7, 1), null, false);
        list.add(t11);
        list.forEach(this::initTutorSchedule);
        tutorRepository.saveAll(list);

        log.info("✓ Initialized 11 tutors (8 APPROVED, 2 PENDING, 1 REJECTED) — Tutor@123");
    }

    private Tutor tutor(Role role, String pw, String email, Gender gender, int age, String province,
                        int exp, double rate, String edu, Subject subject, double rating,
                        VerificationStatus vs, String note, UserStatus status, Set<EducationLevel> levels, TeachingMode mode) {
        String bio = "Gia sư " + subject.getName() + " " + exp + " năm kinh nghiệm. " + sentence(16);
        return Tutor.builder()
                .id(appUtil.generateUUID()).fullName(fullName()).email(email).password(pw).phone(phone())
                .avatarUrl(avatar()).gender(gender).dateOfBirth(dob(age)).address(address(province))
                .userStatus(status).role(role).bio(bio).experienceYears(exp).hourlyRate(BigDecimal.valueOf(rate).setScale(0, RoundingMode.HALF_UP))
                .education(edu).subjects(Set.of(subject)).averageRating(rating)
                .verificationStatus(vs).verificationNote(note)
                .teachingLevels(levels).teachingMode(mode)
                .certificates(new HashSet<>())
                .build();
    }

    private void cert(Tutor tutor, String name, String issuedBy, LocalDate issued, LocalDate expiry, boolean verified) {
        tutor.getCertificates().add(Certificate.builder()
                .id(appUtil.generateUUID()).name(name).issuedBy(issuedBy)
                .issuedDate(issued).expiryDate(expiry)
                .fileUrl("https://res.cloudinary.com/demo/image/upload/sample.jpg")
                .verified(verified).tutor(tutor).build());
    }

    // =========================================================
    // ADMIN
    // =========================================================
    private void initAdmins() {
        if (adminRepository.count() > 0) {
            log.info("Admins already exist. Skip.");
            return;
        }
        Role role = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
        adminRepository.save(Admin.builder()
                .id(appUtil.generateUUID()).fullName("System Admin").email("admin@gmail.com")
                .password(passwordEncoder.encode("Admin@123")).phone(phone())
                .avatarUrl(avatar()).gender(Gender.MALE).dateOfBirth(dob(30))
                .address(address(HCM)).userStatus(UserStatus.ACTIVE)
                .role(role).department("Administration").build());
        log.info("✓ Initialized 1 admin — admin@gmail.com / Admin@123");
    }

    // =========================================================
    // MATCHING FEE CONFIG (tỉ lệ phí nhận lớp)
    // =========================================================
    private void initFeeConfigs() {
        if (matchingFeeConfigRepository.count() > 0) {
            log.info("Fee configs already exist. Skip.");
            return;
        }
        Admin admin = adminRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("admin not found"));
        matchingFeeConfigRepository.saveAll(List.of(
                MatchingFeeConfig.builder().id(appUtil.generateUUID()).feePercentage(0.10).fixedFee(0)
                        .effectiveFrom(LocalDate.of(2025, 1, 1)).effectiveTo(LocalDate.of(2025, 12, 31))
                        .createdByAdmin(admin).build(),
                MatchingFeeConfig.builder().id(appUtil.generateUUID()).feePercentage(0.20).fixedFee(0)
                        .effectiveFrom(LocalDate.of(2026, 1, 1)).effectiveTo(null)
                        .createdByAdmin(admin).build()
        ));
        log.info("✓ Initialized 2 fee configs (10% năm 2025, 20% từ 2026-01-01)");
    }

    // =========================================================
    // POSTS -> APPLICATIONS -> WALLETS -> TRANSACTIONS
    // =========================================================
    private void initMarketplaceData() {
        if (postRepository.count() > 0 || applicationRepository.count() > 0) {
            log.info("Posts/Applications already exist. Skip marketplace data.");
            return;
        }
        Map<String, Subject> sub = subjectByName();
        Map<String, Student> st = studentRepository.findAll().stream()
                .collect(Collectors.toMap(User::getEmail, s -> s));
        Map<String, Tutor> tu = tutorRepository.findAll().stream()
                .collect(Collectors.toMap(User::getEmail, t -> t));

        Student s1 = st.get("student@gmail.com"), s2 = st.get("student2@gmail.com"),
                s3 = st.get("student3@gmail.com"), s4 = st.get("student4@gmail.com"),
                s5 = st.get("student5@gmail.com"), s6 = st.get("student6@gmail.com"),
                s7 = st.get("student7@gmail.com");
        Tutor t1 = tu.get("tutor@gmail.com"), t3 = tu.get("tutor3@gmail.com"), t4 = tu.get("tutor4@gmail.com"),
                t5 = tu.get("tutor5@gmail.com"), t6 = tu.get("tutor6@gmail.com"), t7 = tu.get("tutor7@gmail.com"),
                t8 = tu.get("tutor8@gmail.com"), t9 = tu.get("tutor9@gmail.com");

        final PostType FT = PostType.FIND_TUTOR, FS = PostType.FIND_STUDENT;
        final PostStatus CLOSED = PostStatus.CLOSED, PUB = PostStatus.PUBLISHED;
        final EducationLevel HS = EducationLevel.HIGH_SCHOOL, SEC = EducationLevel.SECONDARY;
        final TeachingMode BOTH = TeachingMode.BOTH, OFF = TeachingMode.OFFLINE, ON = TeachingMode.ONLINE;
        final BudgetUnit PER_SESSION = BudgetUnit.PER_SESSION, PER_HOUR = BudgetUnit.PER_HOUR, PER_MONTH = BudgetUnit.PER_MONTH;

        // ---------- POSTS: FIND_TUTOR (học viên tìm gia sư) ----------
        post("P1", s1, FT, CLOSED, sub.get(S_MATH), HS, BOTH, 1_500_000, 2_000_000, 2, 90, -150, "Tìm gia sư Toán lớp 12", PER_HOUR);
        post("P2", s1, FT, CLOSED, sub.get(S_PHY), HS, OFF, 2_500_000, 3_000_000, 3, 90, -130, "Tìm gia sư Lý ôn thi đại học", PER_HOUR);
        post("P3", s2, FT, CLOSED, sub.get(S_MATH), HS, BOTH, 1_000_000, 1_500_000, 2, 90, -100, "Tìm gia sư Toán lớp 11", PER_HOUR);
        post("P4", s3, FT, CLOSED, sub.get(S_MATH), HS, OFF, 900_000, 1_200_000, 2, 90, -80, "Tìm gia sư Toán lớp 10 tại Đống Đa", PER_HOUR);
        post("P5", s4, FT, CLOSED, sub.get(S_CHE), SEC, OFF, 700_000, 1_000_000, 2, 90, -55, "Tìm gia sư Hóa lớp 9 ôn thi vào 10 chuyên", PER_HOUR);
        post("P6", s2, FT, CLOSED, sub.get(S_ENG), HS, ON, 2_000_000, 2_500_000, 3, 120, -45, "Tìm gia sư IELTS mục tiêu 6.5", PER_HOUR);
        post("P7", s5, FT, PUB, sub.get(S_MATH), SEC, BOTH, 800_000, 1_200_000, 2, 90, 10, "Tìm gia sư Toán lớp 6 xây nền tảng", PER_MONTH);
        post("P8", s1, FT, PUB, sub.get(S_IT), HS, ON, 1_200_000, 1_800_000, 2, 90, 12, "Tìm gia sư Tin học học Python", PER_HOUR);
        post("P9", s2, FT, PUB, sub.get(S_ENG), EducationLevel.OTHER, ON, 1_500_000, 2_000_000, 2, 60, 20, "Cần gia sư tiếng Anh giao tiếp", PER_HOUR);
        post("P10", s1, FT, PUB, sub.get(S_MATH), HS, BOTH, 2_000_000, 2_500_000, 3, 120, 25, "Luyện thi Toán THPT quốc gia - cần gia sư giỏi", PER_HOUR);
        post("P11", s3, FT, PUB, sub.get(S_PHY), HS, BOTH, 1_000_000, 1_500_000, 2, 90, 9, "Tìm gia sư Lý lớp 11 tại Hà Nội", PER_HOUR);
        post("P12", s4, FT, PostStatus.DRAFT, sub.get(S_ENG), SEC, ON, 600_000, 900_000, 2, 60, 30, "Tìm gia sư tiếng Anh lớp 9 (bản nháp)", PER_HOUR);
        postFixed("P13", s5, FT, PostStatus.REMOVED, sub.get(S_CHE), SEC, OFF, 500_000, 800_000, 2, 90, -20,
                "Tìm gia sư Hóa - liên hệ Zalo " + phone() + " để thỏa thuận riêng",
                "Bài đăng bị gỡ do chèn thông tin liên hệ ngoài hệ thống.", PER_MONTH);
        postFixed("P14", s6, FT, PostStatus.REMOVED, sub.get(S_LIT), EducationLevel.UNIVERSITY, ON, 300_000, 500_000, 1, 60, -30,
                "Nhận làm bài hộ tiểu luận Văn", "Bài đăng vi phạm quy định, tài khoản đã bị khóa.", PER_MONTH);
        post("P15", s7, FT, PUB, sub.get(S_BIO), HS, OFF, 1_000_000, 1_400_000, 2, 90, 14, "Tìm gia sư Sinh học khối B", PER_HOUR);
        post("P16", s3, FT, PUB, sub.get(S_LIT), HS, BOTH, 800_000, 1_300_000, 2, 90, 2, "Tìm gia sư Văn lớp 10 - sắp hết hạn", PER_HOUR);
        post("P17", s4, FT, PUB, sub.get(S_ART), EducationLevel.OTHER, OFF, 600_000, 900_000, 1, 60, 18, "Tìm gia sư Piano cho bé", PER_HOUR);
        post("P19", s4, FT, CLOSED, sub.get(S_ENG), SEC, ON, 600_000, 900_000, 2, 60, -90, "Tìm gia sư tiếng Anh lớp 8 học online", PER_HOUR);
        post("P20", s2, FT, CLOSED, sub.get(S_IT), HS, ON, 700_000, 1_000_000, 2, 90, -75, "Tìm gia sư Tin học lớp 11", PER_HOUR);

        // ---------- POSTS: FIND_STUDENT (gia sư tìm học viên) ----------
        post("FS1", t9, FS, CLOSED, sub.get(S_IT), HS, ON, 1_500_000, 2_000_000, 2, 90, -40, "Gia sư Python/C++ nhận học viên THPT", PER_SESSION);
        post("FS2", t8, FS, CLOSED, sub.get(S_LIT), SEC, OFF, 1_200_000, 1_600_000, 2, 90, -55, "Gia sư Ngữ văn nhận học viên THCS tại Đống Đa", PER_SESSION);
        post("FS3", t5, FS, PUB, sub.get(S_PHY), HS, BOTH, 2_000_000, 3_000_000, 3, 90, 15, "Thạc sĩ Vật lý nhận học viên luyện thi đại học", PER_SESSION);
        post("FS4", t1, FS, PUB, sub.get(S_MATH), SEC, BOTH, 1_200_000, 1_800_000, 2, 90, 21, "Gia sư Toán nhận học viên THCS", PER_SESSION);
        post("FS5", t7, FS, PostStatus.DRAFT, sub.get(S_ENG), HS, ON, 2_000_000, 3_000_000, 3, 120, 30, "Gia sư IELTS 8.0 nhận học viên (bản nháp)", PER_SESSION);
        post("FS6", t4, FS, PUB, sub.get(S_CHE), HS, OFF, 1_000_000, 1_500_000, 2, 90, 30, "Gia sư Hóa nhận học viên THPT tại Đà Nẵng", PER_SESSION);

        postRepository.saveAll(posts.values());

        // ---------- APPLICATIONS ----------
        // Bài FIND_TUTOR: tutor ứng tuyển, student = tác giả bài. Bài FIND_STUDENT: student ứng tuyển, tutor = null.
        final ApplicationStatus FIN = ApplicationStatus.FINISHED, REJ = ApplicationStatus.REJECTED,
                CONF = ApplicationStatus.CONFIRMED, CANC = ApplicationStatus.CANCELLED,
                PEND = ApplicationStatus.PENDING, ACC = ApplicationStatus.ACCEPTED;

        // -- đã hoàn thành (FINISHED)
        tutorApp("A1", "P1", t1, FIN, 175);
        tutorApp("A1b", "P1", t3, REJ, 174);
        tutorApp("A1c", "P1", t6, REJ, 173);
        tutorApp("A2", "P2", t5, FIN, 150);
        tutorApp("A2b", "P2", t1, REJ, 149);
        tutorApp("A3", "P3", t1, FIN, 120);
        tutorApp("A4", "P4", t3, FIN, 100);
        tutorApp("A4b", "P4", t6, REJ, 99);
        tutorApp("A5", "P19", t7, FIN, 85);
        tutorApp("A6", "P20", t9, FIN, 70);
        studentApp("A9", "FS2", s3, FIN, 45);
        // -- đang học (CONFIRMED)
        tutorApp("A7", "P5", t4, CONF, 60);
        tutorApp("A8", "P6", t7, CONF, 50);
        studentApp("A10", "FS1", s7, CONF, 35);
        // -- bị hủy / chờ
        tutorApp("A11", "P16", t8, CANC, 14);
        tutorApp("A12", "P11", t5, CANC, 20);
        tutorApp("A12b", "P11", t4, PEND, 19);
        // -- đã được chọn, chờ thanh toán phí (ACCEPTED)
        tutorApp("A13", "P7", t1, ACC, 6);
        tutorApp("A13b", "P7", t6, REJ, 5);
        tutorApp("A13c", "P7", t3, REJ, 4);
        tutorApp("A14", "P8", t9, ACC, 3);
        // -- nhiều ứng viên chờ chọn (demo AI top 5, 6 ứng viên)
        tutorApp("A15a", "P10", t1, PEND, 3);
        tutorApp("A15b", "P10", t3, PEND, 3);
        tutorApp("A15c", "P10", t6, PEND, 2);
        tutorApp("A15d", "P10", t5, PEND, 2);
        tutorApp("A15e", "P10", t8, PEND, 1);
        tutorApp("A15f", "P10", t4, PEND, 1);
        tutorApp("A16a", "P9", t7, PEND, 2);
        tutorApp("A16b", "P9", t8, PEND, 1);
        studentApp("A17a", "FS3", s1, PEND, 4);
        studentApp("A17b", "FS3", s2, PEND, 3);
        studentApp("A17c", "FS3", s5, PEND, 2);
        studentApp("A18", "FS4", s3, PEND, 2);
        applicationRepository.saveAll(apps.values());

        // ---------- TRANSACTIONS (+ ví) ----------
        tx(t1, null, TransactionType.TOP_UP, bd(2_000_000), TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER, 178, null);
        tx(t1, null, TransactionType.TOP_UP, bd(500_000), TransactionStatus.PENDING, PaymentMethod.MOMO, 1, "MOMO-DEMO-PENDING-001");
        tx(t1, null, TransactionType.TOP_UP, bd(1_000_000), TransactionStatus.FAILED, PaymentMethod.ZALOPAY, 30, "ZLP-DEMO-FAILED-001");
        commission(t1, "A1", 170);
        commission(t1, "A3", 112);

        tx(t5, null, TransactionType.TOP_UP, bd(3_000_000), TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER, 152, null);
        commission(t5, "A2", 145);
        tx(t5, apps.get("A12"), TransactionType.COMMISSION, fee(posts.get("P11")),
                TransactionStatus.CANCELLED, PaymentMethod.WALLET, 18, null);

        tx(t3, null, TransactionType.TOP_UP, bd(600_000), TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER, 102, null);
        commission(t3, "A4", 95);

        tx(t7, null, TransactionType.TOP_UP, bd(2_000_000), TransactionStatus.SUCCESS, PaymentMethod.ZALOPAY, 88, "ZLP-DEMO-SUCCESS-001");
        commission(t7, "A5", 80);
        commission(t7, "A8", 45);

        tx(t9, null, TransactionType.TOP_UP, bd(800_000), TransactionStatus.SUCCESS, PaymentMethod.MOMO, 72, "MOMO-DEMO-SUCCESS-001");
        commission(t9, "A6", 65);
        commission(t9, "A10", 30);

        tx(t4, null, TransactionType.TOP_UP, bd(1_000_000), TransactionStatus.SUCCESS, PaymentMethod.MOMO, 62, "MOMO-DEMO-SUCCESS-002");
        commission(t4, "A7", 55);

        tx(t8, null, TransactionType.TOP_UP, bd(800_000), TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER, 47, null);
        commission(t8, "A9", 40);
        tx(t8, apps.get("A11"), TransactionType.COMMISSION, fee(posts.get("P16")),
                TransactionStatus.REFUNDED, PaymentMethod.WALLET, 12, null);

        tx(t6, null, TransactionType.TOP_UP, bd(100_000), TransactionStatus.SUCCESS, PaymentMethod.BANK_TRANSFER, 20, null);

        // Mỗi gia sư có ví (kể cả PENDING/REJECTED -> số dư 0)
        List<Wallet> wallets = tutorRepository.findAll().stream()
                .map(t -> Wallet.builder().id(appUtil.generateUUID()).tutor(t)
                        .balance(ledger.getOrDefault(t.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                        .build())
                .toList();
        walletRepository.saveAll(wallets);
        transactionRepository.saveAll(transactions);

        BigDecimal revenue = transactions.stream()
                .filter(x -> x.getType() == TransactionType.COMMISSION && x.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("✓ Initialized marketplace data: {} posts, {} applications, {} wallets, {} transactions",
                posts.size(), apps.size(), wallets.size(), transactions.size());
        log.info("  Doanh thu phí nhận lớp (COMMISSION SUCCESS) = {} VND", revenue.toPlainString());
        log.info("  Demo AI top 5: bài 'Luyện thi Toán THPT quốc gia' (student@gmail.com) có 6 ứng viên PENDING");
        log.info("  Demo thanh toán: tutor1 được chọn bài 'Toán lớp 6' (đủ số dư); tutor9 bài 'Python' (THIẾU số dư -> nạp thêm)");
    }

    // ---------- helpers: post / application / transaction ----------

    /**
     * Địa chỉ bài đăng lấy theo địa chỉ tác giả; nội dung do Faker sinh.
     */
    private void post(String key, User author, PostType type, PostStatus status, Subject subject,
                      EducationLevel level, TeachingMode mode, long min, long max,
                      int sessions, int minutes, int deadlineDays, String title, BudgetUnit unit) {
        postFixed(key, author, type, status, subject, level, mode, min, max, sessions, minutes,
                deadlineDays, title, sentence(20), unit);
    }

    private void postFixed(String key, User author, PostType type, PostStatus status, Subject subject,
                           EducationLevel level, TeachingMode mode, long min, long max,
                           int sessions, int minutes, int deadlineDays, String title, String content, BudgetUnit unit) {
        posts.put(key, Post.builder()
                .id(appUtil.generateUUID()).title(title).content(content).type(type).status(status)
                .author(author).subject(subject).educationLevel(level).teachingMode(mode)
                .address(author.getAddress()).minBudget(bd(min)).maxBudget(bd(max)).budgetUnit(unit)
                .sessionsPerWeek(sessions).durationMinutes(minutes)
                .deadline(Instant.now().plus(deadlineDays, ChronoUnit.DAYS))
                .build());
    }

    private void tutorApp(String key, String postKey, Tutor tutor, ApplicationStatus status, int daysAgo) {
        BigDecimal amount = BigDecimal.valueOf(
                ThreadLocalRandom.current().nextLong(60_000, 600_001)
        );
        Post p = posts.get(postKey);
        apps.put(key, Application.builder()
                .id(appUtil.generateUUID()).applicationStatus(status).message(sentence(14)).proposedFee(amount)
                .appliedAt(ago(daysAgo)).post(p).student((Student) p.getAuthor()).tutor(tutor).build());
    }

    private void studentApp(String key, String postKey, Student student, ApplicationStatus status, int daysAgo) {
        apps.put(key, Application.builder()
                .id(appUtil.generateUUID()).applicationStatus(status).message(sentence(14))
                .appliedAt(ago(daysAgo)).post(posts.get(postKey)).student(student).tutor(null).build());
    }

    private void commission(Tutor tutor, String appKey, int daysAgo) {
        Application a = apps.get(appKey);
        tx(tutor, a, TransactionType.COMMISSION, fee(a.getPost()), TransactionStatus.SUCCESS,
                PaymentMethod.WALLET, daysAgo, null);
    }

    private void tx(Tutor tutor, Application application, TransactionType type, BigDecimal amount,
                    TransactionStatus status, PaymentMethod method, int daysAgo, String providerOrderId) {
        Transaction t = Transaction.builder()
                .id(appUtil.generateUUID()).type(type).amount(amount).status(status).paymentMethod(method)
                .transactionDate(ago(daysAgo)).providerOrderId(providerOrderId)
                .providerTransId(status == TransactionStatus.SUCCESS && providerOrderId != null
                        ? "TRANS-" + providerOrderId : null)
                .tutor(tutor).application(application).build();
        transactions.add(t);
        if (status == TransactionStatus.SUCCESS) {
            BigDecimal delta = type == TransactionType.TOP_UP ? amount : amount.negate();
            ledger.merge(tutor.getId(), delta, BigDecimal::add);
        }
    }

    private BigDecimal fee(Post post) {
        return post.getMaxBudget().multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // REVIEWS (chỉ cho application FINISHED)
    // =========================================================
    private void initReviews() {
        if (apps.isEmpty() || reviewRepository.count() > 0) {
            log.info("Reviews skipped.");
            return;
        }
        review("R1", "A1", 5);
        review("R2", "A2", 5);
        review("R3", "A3", 4);
        review("R4", "A4", 4);
        review("R5", "A5", 3);
        review("R6", "A6", 2);
        review("R7", "A9", 5);
        reviewRepository.saveAll(reviews.values());
        log.info("✓ Initialized {} reviews", reviews.size());
    }

    private void review(String key, String appKey, int rating) {
        Application a = apps.get(appKey);
        Tutor tutor = a.getTutor() != null ? a.getTutor() : (Tutor) a.getPost().getAuthor();
        reviews.put(key, Review.builder()
                .id(appUtil.generateUUID()).rating(rating).comment(sentence(15))
                .student(a.getStudent()).tutor(tutor).application(a).build());
    }

    // =========================================================
    // REVIEW COMPLAINTS (khiếu nại đánh giá)
    // =========================================================
    private void initReviewComplaints() {
        if (reviews.isEmpty() || reviewComplaintRepository.count() > 0) {
            log.info("Review complaints skipped.");
            return;
        }
        Admin admin = adminRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("admin not found"));

        reviewComplaintRepository.saveAll(List.of(
                complaint("R3", ComplaintStatus.PENDING, null),
                complaint("R5", ComplaintStatus.APPROVED, admin),
                complaint("R6", ComplaintStatus.REJECTED, admin)
        ));
        log.info("✓ Initialized 3 review complaints (PENDING / APPROVED / REJECTED)");
    }

    private ReviewComplaint complaint(String reviewKey, ComplaintStatus status, Admin admin) {
        Review r = reviews.get(reviewKey);
        return ReviewComplaint.builder().id(appUtil.generateUUID()).reason(sentence(14)).status(status)
                .review(r).tutor(r.getTutor()).admin(admin).build();
    }

    // =========================================================
    // FAVORITES
    // =========================================================
    private void initFavorites() {
        if (favoriteRepository.count() > 0) {
            log.info("Favorites already exist. Skip.");
            return;
        }
        Map<String, Student> st = studentRepository.findAll().stream().collect(Collectors.toMap(User::getEmail, s -> s));
        Map<String, Tutor> tu = tutorRepository.findAll().stream().collect(Collectors.toMap(User::getEmail, t -> t));

        List<Favorite> list = new ArrayList<>();
        list.add(fav(st.get("student@gmail.com"), tu.get("tutor@gmail.com")));
        list.add(fav(st.get("student@gmail.com"), tu.get("tutor5@gmail.com")));
        list.add(fav(st.get("student@gmail.com"), tu.get("tutor7@gmail.com")));
        list.add(fav(st.get("student2@gmail.com"), tu.get("tutor@gmail.com")));
        list.add(fav(st.get("student2@gmail.com"), tu.get("tutor7@gmail.com")));
        list.add(fav(st.get("student2@gmail.com"), tu.get("tutor8@gmail.com")));
        list.add(fav(st.get("student3@gmail.com"), tu.get("tutor3@gmail.com")));
        list.add(fav(st.get("student3@gmail.com"), tu.get("tutor8@gmail.com")));
        list.add(fav(st.get("student4@gmail.com"), tu.get("tutor4@gmail.com")));
        list.add(fav(st.get("student5@gmail.com"), tu.get("tutor@gmail.com")));
        list.add(fav(st.get("student5@gmail.com"), tu.get("tutor5@gmail.com")));
        list.add(fav(st.get("student7@gmail.com"), tu.get("tutor9@gmail.com")));
        favoriteRepository.saveAll(list);
        log.info("✓ Initialized {} favorites", list.size());
    }

    private Favorite fav(User user, Tutor tutor) {
        return Favorite.builder().id(appUtil.generateUUID()).user(user).tutor(tutor).build();
    }

    // =========================================================
    // CHAT (chỉ cho cặp đã thanh toán: CONFIRMED / FINISHED)
    // =========================================================
    private void initChats() {
        if (apps.isEmpty() || chatChannelRepository.count() > 0) {
            log.info("Chats skipped.");
            return;
        }
        Set<String> seen = new HashSet<>();
        List<ChatChannel> channels = new ArrayList<>();
        List<Message> messages = new ArrayList<>();

        for (Application a : apps.values()) {
            if (a.getApplicationStatus() != ApplicationStatus.CONFIRMED
                    && a.getApplicationStatus() != ApplicationStatus.FINISHED) {
                continue;
            }
            Student student = a.getStudent();
            Tutor tutor = a.getTutor() != null ? a.getTutor() : (Tutor) a.getPost().getAuthor();
            if (!seen.add(tutor.getId() + ":" + student.getId())) {
                continue;
            }
            boolean finished = a.getApplicationStatus() == ApplicationStatus.FINISHED;
            Instant base = a.getAppliedAt().plus(5, ChronoUnit.DAYS);

            ChatChannel ch = ChatChannel.builder().id(appUtil.generateUUID())
                    .name(tutor.getFullName() + " & " + student.getFullName())
                    .sender(tutor).receiver(student).build();

            // lượt thoại xen kẽ gia sư / học viên, nội dung do Faker sinh
            boolean[] fromTutorTurns = {true, false, true, false, true};
            Instant last = base;
            for (int i = 0; i < fromTutorTurns.length; i++) {
                last = base.plus(i * 20L, ChronoUnit.MINUTES);
                // tin nhắn cuối của lớp đang học để chưa đọc -> demo badge tin nhắn mới
                boolean isRead = finished || i < fromTutorTurns.length - 1;
                messages.add(Message.builder()
                        .id(appUtil.generateUUID()).content(sentence(10)).isRead(isRead).sentAt(last)
                        .chatChannel(ch).sender(fromTutorTurns[i] ? tutor : student).build());
            }
            ch.setLastMessageAt(last);
            channels.add(ch);
        }
        chatChannelRepository.saveAll(channels);
        messageRepository.saveAll(messages);
        log.info("✓ Initialized {} chat channels, {} messages", channels.size(), messages.size());
    }

    // =========================================================
    // NOTIFICATIONS (tên người dùng lấy từ dữ liệu đã sinh)
    // =========================================================
    private void initNotifications() {
        if (notificationRepository.count() > 0) {
            log.info("Notifications already exist. Skip.");
            return;
        }
        Map<String, User> u = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getEmail, x -> x));
        List<Notification> list = new ArrayList<>();

        // Chào mừng cho tất cả tài khoản
        for (User user : u.values()) {
            list.add(notif(user, NotificationType.SYSTEM, "Chào mừng bạn đến với TutorLink",
                    "Tài khoản của bạn đã được tạo thành công.", true));
        }

        // --- Học viên ---
        User s1 = u.get("student@gmail.com");
        list.add(notif(s1, NotificationType.POST, "Bài đăng đã được công khai",
                "Bài \"Luyện thi Toán THPT quốc gia - cần gia sư giỏi\" đã hiển thị với mọi người.", true));
        list.add(notif(s1, NotificationType.APPLICATION, "Có 6 lượt đăng ký mới",
                "Bài \"Luyện thi Toán THPT quốc gia\" có 6 gia sư đăng ký. Xem gợi ý AI top 5 để chọn.", false));
        list.add(notif(s1, NotificationType.MESSAGE, "Tin nhắn mới",
                name(u, "tutor@gmail.com") + " đã gửi tin nhắn cho bạn.", false));
        list.add(notif(s1, NotificationType.REVIEW, "Hãy đánh giá gia sư",
                "Lớp Lý với " + name(u, "tutor5@gmail.com") + " đã kết thúc. Chia sẻ trải nghiệm của bạn nhé.", true));

        User s2 = u.get("student2@gmail.com");
        list.add(notif(s2, NotificationType.APPLICATION, "Có 2 lượt đăng ký mới",
                "Bài \"Cần gia sư tiếng Anh giao tiếp\" vừa có thêm 2 gia sư đăng ký.", false));
        list.add(notif(s2, NotificationType.MESSAGE, "Tin nhắn mới",
                name(u, "tutor7@gmail.com") + " đã gửi tin nhắn cho bạn.", false));
        list.add(notif(s2, NotificationType.TRANSACTION, "Kết nối thành công",
                "Gia sư " + name(u, "tutor7@gmail.com") + " đã thanh toán phí nhận lớp. Thông tin liên hệ đã được mở.", true));

        User s3 = u.get("student3@gmail.com");
        list.add(notif(s3, NotificationType.POST, "Bài đăng sắp hết hạn",
                "Bài \"Tìm gia sư Văn lớp 10\" sẽ hết hạn nhận đăng ký trong 2 ngày.", false));
        list.add(notif(s3, NotificationType.MATCHING, "Gia sư đã hủy nhận lớp",
                "Gia sư " + name(u, "tutor5@gmail.com") + " không nhận lớp Lý 11. Bạn có thể chọn ứng viên khác.", false));

        User s4 = u.get("student4@gmail.com");
        list.add(notif(s4, NotificationType.TRANSACTION, "Kết nối thành công",
                "Gia sư " + name(u, "tutor4@gmail.com") + " đã thanh toán phí nhận lớp. Bạn có thể nhắn tin ngay.", true));

        User s5 = u.get("student5@gmail.com");
        list.add(notif(s5, NotificationType.MATCHING, "Bạn đã chọn gia sư",
                "Bạn đã chọn " + name(u, "tutor@gmail.com") + ". Đang chờ gia sư thanh toán phí nhận lớp.", false));
        list.add(notif(s5, NotificationType.POST, "Bài đăng bị gỡ",
                "Bài \"Tìm gia sư Hóa - liên hệ Zalo...\" đã bị gỡ do chèn thông tin liên hệ ngoài hệ thống.", false));

        User s6 = u.get("student6@gmail.com");
        list.add(notif(s6, NotificationType.SYSTEM, "Tài khoản bị khóa",
                "Tài khoản của bạn bị khóa do vi phạm quy định đăng bài.", false));

        // --- Gia sư ---
        User t1 = u.get("tutor@gmail.com");
        list.add(notif(t1, NotificationType.SYSTEM, "Hồ sơ đã được duyệt",
                "Hồ sơ giảng dạy của bạn đã được Admin duyệt. Bạn có thể đăng ký nhận lớp.", true));
        list.add(notif(t1, NotificationType.MATCHING, "Bạn được chọn nhận lớp",
                "Bạn được chọn cho lớp \"Tìm gia sư Toán lớp 6\". Hãy thanh toán phí nhận lớp 240.000đ để mở liên hệ.", false));
        list.add(notif(t1, NotificationType.TRANSACTION, "Nạp tiền đang xử lý",
                "Giao dịch nạp 500.000đ qua MoMo đang chờ xác nhận.", false));
        list.add(notif(t1, NotificationType.REVIEW, "Bạn nhận được đánh giá mới",
                "Một học viên vừa đánh giá bạn 4 sao. Bạn có thể gửi khiếu nại nếu thấy chưa phù hợp.", true));
        list.add(notif(t1, NotificationType.APPLICATION, "Có học viên đăng ký lớp của bạn",
                name(u, "student3@gmail.com") + " vừa đăng ký bài \"Gia sư Toán nhận học viên THCS\".", false));

        User t2 = u.get("tutor2@gmail.com");
        list.add(notif(t2, NotificationType.SYSTEM, "Hồ sơ đang chờ duyệt",
                "Hồ sơ của bạn đang được Admin xem xét, bạn sẽ nhận thông báo khi có kết quả.", false));

        User t3 = u.get("tutor3@gmail.com");
        list.add(notif(t3, NotificationType.TRANSACTION, "Thanh toán phí thành công",
                "Bạn đã thanh toán phí nhận lớp 240.000đ. Thông tin liên hệ đã được mở.", true));
        list.add(notif(t3, NotificationType.APPLICATION, "Ứng tuyển không được chọn",
                "Rất tiếc, bài \"Tìm gia sư Toán lớp 6\" đã chọn gia sư khác.", false));

        User t4 = u.get("tutor4@gmail.com");
        list.add(notif(t4, NotificationType.TRANSACTION, "Thanh toán phí thành công",
                "Bạn đã thanh toán phí nhận lớp 200.000đ cho lớp Hóa 9.", true));
        list.add(notif(t4, NotificationType.MESSAGE, "Tin nhắn mới",
                name(u, "student4@gmail.com") + " đã gửi tin nhắn cho bạn.", false));

        User t5 = u.get("tutor5@gmail.com");
        list.add(notif(t5, NotificationType.APPLICATION, "Có 3 học viên đăng ký",
                "Bài \"Thạc sĩ Vật lý nhận học viên luyện thi đại học\" có 3 lượt đăng ký mới.", false));
        list.add(notif(t5, NotificationType.MATCHING, "Lượt nhận lớp bị hủy",
                "Bạn đã từ chối nhận lớp Lý 11. Không phát sinh phí.", true));

        User t7 = u.get("tutor7@gmail.com");
        list.add(notif(t7, NotificationType.REVIEW, "Khiếu nại được chấp nhận",
                "Admin đã chấp nhận khiếu nại của bạn về đánh giá 3 sao.", false));
        list.add(notif(t7, NotificationType.MESSAGE, "Tin nhắn mới",
                name(u, "student2@gmail.com") + " đã gửi tin nhắn cho bạn.", false));

        User t8 = u.get("tutor8@gmail.com");
        list.add(notif(t8, NotificationType.TRANSACTION, "Hoàn phí nhận lớp",
                "Phí nhận lớp 260.000đ của lớp \"Văn lớp 10\" đã được hoàn lại vào ví.", true));

        User t9 = u.get("tutor9@gmail.com");
        list.add(notif(t9, NotificationType.MATCHING, "Bạn được chọn nhận lớp",
                "Bạn được chọn cho lớp \"Tìm gia sư Tin học học Python\". Cần thanh toán 360.000đ nhưng số dư ví chưa đủ, hãy nạp thêm.", false));
        list.add(notif(t9, NotificationType.REVIEW, "Khiếu nại bị từ chối",
                "Admin đã xem xét và giữ nguyên đánh giá 2 sao của học viên.", false));

        User t10 = u.get("tutor10@gmail.com");
        list.add(notif(t10, NotificationType.SYSTEM, "Hồ sơ đang chờ duyệt",
                "Hồ sơ của bạn đang được Admin xem xét.", false));

        User t11 = u.get("tutor11@gmail.com");
        list.add(notif(t11, NotificationType.SYSTEM, "Hồ sơ bị từ chối",
                "Ảnh chụp bằng cấp bị mờ. Vui lòng cập nhật lại hồ sơ và gửi duyệt lần nữa.", false));

        // --- Admin ---
        User admin = u.get("admin@gmail.com");
        list.add(notif(admin, NotificationType.SYSTEM, "Có hồ sơ gia sư chờ duyệt",
                "Hiện có 2 hồ sơ gia sư ở trạng thái PENDING cần xem xét.", false));
        list.add(notif(admin, NotificationType.REVIEW, "Có khiếu nại đánh giá mới",
                "Gia sư " + name(u, "tutor@gmail.com") + " vừa gửi khiếu nại về một đánh giá 4 sao.", false));

        notificationRepository.saveAll(list);
        log.info("✓ Initialized {} notifications", list.size());
    }

    private Notification notif(User recipient, NotificationType type, String title, String content, boolean read) {
        return Notification.builder().id(appUtil.generateUUID()).title(title).content(content)
                .notificationType(type).isRead(read).recipient(recipient).build();
    }

    private String name(Map<String, User> users, String email) {
        return users.get(email).getFullName();
    }

    // =========================================================
    // REVENUE REPORTS (báo cáo doanh thu theo tháng, tính từ giao dịch COMMISSION SUCCESS)
    // =========================================================
    private void initRevenueReports() {
        if (transactions.isEmpty() || revenueReportRepository.count() > 0) {
            log.info("Revenue reports skipped.");
            return;
        }
        Admin admin = adminRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("admin not found"));
        YearMonth current = YearMonth.now(ZONE);

        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .filter(t -> t.getType() == TransactionType.COMMISSION && t.getStatus() == TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate().atZone(ZONE)),
                        TreeMap::new, Collectors.toList()));

        List<RevenueReport> reports = new ArrayList<>();
        byMonth.forEach((month, list) -> {
            if (month.equals(current)) {
                return; // tháng hiện tại chưa chốt
            }
            double total = list.stream().map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
            reports.add(RevenueReport.builder().id(appUtil.generateUUID())
                    .periodStart(month.atDay(1)).periodEnd(month.atEndOfMonth())
                    .totalRevenue(total).totalTransactions(list.size())
                    .generatedByAdmin(admin).build());
        });
        revenueReportRepository.saveAll(reports);
        log.info("✓ Initialized {} monthly revenue reports", reports.size());
    }

    // =========================================================
    // FAKER HELPERS
    // =========================================================
    private Map<String, Subject> subjectByName() {
        return subjectRepository.findAll().stream()
                .collect(Collectors.toMap(Subject::getName, s -> s, (a, b) -> a));
    }

    private String fullName() {
        return faker.name().fullName();
    }

    /**
     * SĐT di động VN: 09xxxxxxxx.
     */
    private String phone() {
        return "09" + faker.number().digits(8);
    }

    private LocalDate dob(int age) {
        return faker.date().birthday(age, age).toInstant().atZone(ZONE).toLocalDate();
    }

    private String avatar() {
        return "https://i.pravatar.cc/300?img=" + faker.number().numberBetween(1, 70);
    }

    private String sentence(int words) {
        return faker.lorem().sentence(words);
    }

    private Address address(String province) {
        return Address.builder()
                .street(faker.address().streetAddress())
                .ward("Phường " + faker.number().numberBetween(1, 15))
                .province(province)
                .build();
    }

    private BigDecimal bd(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Instant ago(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }
}