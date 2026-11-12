package org.group3.tutorlink.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.chat.entity.Message;
import org.group3.tutorlink.features.chat.enums.MediaType;
import org.group3.tutorlink.features.favorite.entity.Favorite;
import org.group3.tutorlink.features.notification.entity.Notification;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.payment.entity.MatchingFeeConfig;
import org.group3.tutorlink.features.payment.entity.RevenueReport;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.BudgetUnit;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.review.entity.Review;
import org.group3.tutorlink.features.review.entity.ReviewComplaint;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.AvailableSlot;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nạp dữ liệu mẫu cho môi trường dev (profile "dev") từ file JSON.
 * <p>
 * Đặt file {@code seed-data.json} vào {@code src/main/resources/data/seed-data.json}.
 * Chỉ chạy khi DB còn trống (bảng roles chưa có dòng nào), nên chạy lại nhiều lần vẫn an toàn.
 * <p>
 * Chạy với {@code @Order(1)} để các runner phụ thuộc dữ liệu seed (ví dụ TutorReindexRunner,
 * {@code @Order(2)}) luôn chạy sau khi seed hoàn tất.
 * <p>
 * Tài khoản demo:
 * ADMIN   : admin@gmail.com                              / Admin@123
 * STUDENT : student@gmail.com, student2..9@gmail.com     / Student@123
 *           match.student1..2@gmail.com (dữ liệu test xếp hạng) / Student@123
 * TUTOR   : tutor@gmail.com,   tutor2..11@gmail.com      / Tutor@123
 *           match.tutor1..20@gmail.com (dữ liệu test xếp hạng)  / Tutor@123
 * <p>
 * Lưu ý: createdAt/updatedAt dùng @CreationTimestamp/@UpdateTimestamp nên Hibernate sẽ ghi đè
 * khi persist; vì vậy sau khi lưu xong, class này UPDATE lại 2 cột thời gian theo đúng giá trị
 * trong JSON (giữ nguyên thứ tự thời gian của dữ liệu).
 * Không cần dependency datafaker.
 */
@Slf4j
@Order(1)
@Profile("dev")
@RequiredArgsConstructor
@Component
public class InitData implements CommandLineRunner {

    private static final String SEED_FILE = "data/seed-data.json";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txManager;

    /** table -> danh sách [id, createdAt, updatedAt] để chỉnh lại sau khi persist */
    private final Map<String, List<Object[]>> stamps = new LinkedHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        Long roles = em.createQuery("select count(r) from Role r", Long.class).getSingleResult();
        if (roles > 0) {
            log.info("InitData: DB đã có dữ liệu, bỏ qua.");
            return;
        }
        JsonNode root;
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            root = new ObjectMapper().readTree(in);
        }
        new TransactionTemplate(txManager).executeWithoutResult(status -> load(root));
        applyTimestamps();
        log.info("InitData: đã nạp xong dữ liệu mẫu {}", root.get("_meta").get("counts"));
    }

    // ------------------------------------------------------------------ load
    private void load(JsonNode root) {
        Map<UUID, Role> roles = new HashMap<>();
        Map<UUID, Subject> subjects = new HashMap<>();
        Map<UUID, Admin> admins = new HashMap<>();
        Map<UUID, Student> students = new HashMap<>();
        Map<UUID, Tutor> tutors = new HashMap<>();
        Map<UUID, org.group3.tutorlink.features.user.entity.User> users = new HashMap<>();
        Map<UUID, Post> posts = new HashMap<>();
        Map<UUID, Application> apps = new HashMap<>();
        Map<UUID, Review> reviews = new HashMap<>();
        Map<UUID, ChatChannel> channels = new HashMap<>();
        Map<UUID, Message> messages = new HashMap<>();

        for (JsonNode n : root.get("roles")) {
            Role e = Role.builder().id(id(n, "id")).name(RoleName.valueOf(s(n, "name"))).description(s(n, "description")).build();
            roles.put(e.getId(), persist("roles", n, e));
        }
        for (JsonNode n : root.get("subjects")) {
            Subject e = Subject.builder().id(id(n, "id")).name(s(n, "name")).description(s(n, "description")).category(s(n, "category")).build();
            subjects.put(e.getId(), persist("subjects", n, e));
        }
        for (JsonNode n : root.get("admins")) {
            Admin e = Admin.builder()
                    .id(id(n, "id")).fullName(s(n, "fullName")).email(s(n, "email")).password(s(n, "password"))
                    .phone(s(n, "phone")).avatarUrl(s(n, "avatarUrl")).gender(Gender.valueOf(s(n, "gender")))
                    .dateOfBirth(date(n, "dateOfBirth")).address(address(n.get("address")))
                    .userStatus(UserStatus.valueOf(s(n, "userStatus"))).role(roles.get(id(n, "roleId")))
                    .department(s(n, "department")).build();
            admins.put(e.getId(), persist("users", n, e));
            users.put(e.getId(), e);
        }
        for (JsonNode n : root.get("students")) {
            Student e = Student.builder()
                    .id(id(n, "id")).fullName(s(n, "fullName")).email(s(n, "email")).password(s(n, "password"))
                    .phone(s(n, "phone")).avatarUrl(s(n, "avatarUrl")).gender(Gender.valueOf(s(n, "gender")))
                    .dateOfBirth(date(n, "dateOfBirth")).address(address(n.get("address")))
                    .userStatus(UserStatus.valueOf(s(n, "userStatus"))).role(roles.get(id(n, "roleId")))
                    .grade(s(n, "grade")).learningGoal(s(n, "learningGoal")).build();
            students.put(e.getId(), persist("users", n, e));
            users.put(e.getId(), e);
        }
        for (JsonNode n : root.get("tutors")) {
            Set<EducationLevel> levels = new HashSet<>();
            n.get("teachingLevels").forEach(l -> levels.add(EducationLevel.valueOf(l.asText())));
            List<AvailableSlot> slots = new ArrayList<>();
            n.get("availableSlots").forEach(sl -> slots.add(AvailableSlot.builder()
                    .dayOfWeek(DayOfWeek.valueOf(s(sl, "dayOfWeek")))
                    .startTime(Instant.parse(s(sl, "startTime"))).endTime(Instant.parse(s(sl, "endTime"))).build()));
            List<String> areas = new ArrayList<>();
            n.get("teachingAreas").forEach(a -> areas.add(a.asText()));
            Set<Subject> subs = new HashSet<>();
            n.get("subjectIds").forEach(sid -> subs.add(subjects.get(UUID.fromString(sid.asText()))));
            Tutor e = Tutor.builder()
                    .id(id(n, "id")).fullName(s(n, "fullName")).email(s(n, "email")).password(s(n, "password"))
                    .phone(s(n, "phone")).avatarUrl(s(n, "avatarUrl")).gender(Gender.valueOf(s(n, "gender")))
                    .dateOfBirth(date(n, "dateOfBirth")).address(address(n.get("address")))
                    .userStatus(UserStatus.valueOf(s(n, "userStatus"))).role(roles.get(id(n, "roleId")))
                    .bio(s(n, "bio")).experienceYears(n.get("experienceYears").asInt())
                    .hourlyRate(n.get("hourlyRate").decimalValue()).averageRating(n.get("averageRating").asDouble())
                    .verificationStatus(VerificationStatus.valueOf(s(n, "verificationStatus")))
                    .verificationNote(s(n, "verificationNote")).education(s(n, "education"))
                    .teachingLevels(levels).teachingMode(TeachingMode.valueOf(s(n, "teachingMode")))
                    .availableSlots(slots).teachingAreas(areas).subjects(subs).build();
            tutors.put(e.getId(), persist("users", n, e));
            users.put(e.getId(), e);
        }
        em.flush();

        for (JsonNode n : root.get("certificates")) {
            persist("certificates", n, Certificate.builder()
                    .id(id(n, "id")).name(s(n, "name")).issuedBy(s(n, "issuedBy"))
                    .issuedDate(date(n, "issuedDate")).expiryDate(date(n, "expiryDate"))
                    .fileUrl(s(n, "fileUrl")).verified(n.get("verified").asBoolean())
                    .tutor(tutors.get(id(n, "tutorId"))).build());
        }
        for (JsonNode n : root.get("matchingFeeConfigs")) {
            persist("matching_fee_configs", n, MatchingFeeConfig.builder()
                    .id(id(n, "id")).feePercentage(n.get("feePercentage").asDouble()).fixedFee(n.get("fixedFee").asDouble())
                    .effectiveFrom(date(n, "effectiveFrom")).effectiveTo(date(n, "effectiveTo"))
                    .createdByAdmin(admins.get(id(n, "createdByAdminId"))).build());
        }

        for (JsonNode n : root.get("posts")) {
            JsonNode a = n.get("address");
            Post e = Post.builder()
                    .id(id(n, "id")).title(s(n, "title")).content(s(n, "content"))
                    .type(PostType.valueOf(s(n, "type"))).status(PostStatus.valueOf(s(n, "status")))
                    .author(users.get(id(n, "authorId"))).subject(subjects.get(id(n, "subjectId")))
                    .educationLevel(EducationLevel.valueOf(s(n, "educationLevel")))
                    .teachingMode(TeachingMode.valueOf(s(n, "teachingMode")))
                    .address(a == null || a.isNull() ? null : address(a))
                    .minBudget(dec(n, "minBudget")).maxBudget(dec(n, "maxBudget"))
                    .budgetUnit(BudgetUnit.valueOf(s(n, "budgetUnit")))
                    .sessionsPerWeek(n.get("sessionsPerWeek").asInt()).durationMinutes(n.get("durationMinutes").asInt())
                    .deadline(instant(n, "deadline")).build();
            posts.put(e.getId(), persist("posts", n, e));
        }
        em.flush();

        for (JsonNode n : root.get("applications")) {
            Application e = Application.builder()
                    .id(id(n, "id")).applicationStatus(ApplicationStatus.valueOf(s(n, "applicationStatus")))
                    .message(s(n, "message")).appliedAt(instant(n, "appliedAt"))
                    .post(posts.get(id(n, "postId"))).student(students.get(id(n, "studentId")))
                    .tutor(tutors.get(id(n, "tutorId"))).processedByAdmin(admins.get(id(n, "processedByAdminId")))
                    .proposedFee(dec(n, "proposedFee")).build();
            apps.put(e.getId(), persist("applications", n, e));
        }
        for (JsonNode n : root.get("wallets")) {
            persist("wallets", n, Wallet.builder().id(id(n, "id")).balance(n.get("balance").decimalValue())
                    .tutor(tutors.get(id(n, "tutorId"))).build());
        }
        em.flush();

        for (JsonNode n : root.get("transactions")) {
            persist("transactions", n, Transaction.builder()
                    .id(id(n, "id")).amount(n.get("amount").decimalValue())
                    .type(TransactionType.valueOf(s(n, "type"))).status(TransactionStatus.valueOf(s(n, "status")))
                    .paymentMethod(PaymentMethod.valueOf(s(n, "paymentMethod")))
                    .transactionDate(instant(n, "transactionDate"))
                    .providerOrderId(s(n, "providerOrderId")).providerTransId(s(n, "providerTransId"))
                    .tutor(tutors.get(id(n, "tutorId"))).application(apps.get(id(n, "applicationId"))).build());
        }
        for (JsonNode n : root.get("reviews")) {
            Review e = Review.builder().id(id(n, "id")).rating(n.get("rating").asInt()).comment(s(n, "comment"))
                    .student(students.get(id(n, "studentId"))).tutor(tutors.get(id(n, "tutorId")))
                    .application(apps.get(id(n, "applicationId"))).build();
            reviews.put(e.getId(), persist("reviews", n, e));
        }
        em.flush();
        for (JsonNode n : root.get("reviewComplaints")) {
            persist("review_complaints", n, ReviewComplaint.builder()
                    .id(id(n, "id")).reason(s(n, "reason")).status(ComplaintStatus.valueOf(s(n, "status")))
                    .review(reviews.get(id(n, "reviewId"))).tutor(tutors.get(id(n, "tutorId")))
                    .admin(admins.get(id(n, "adminId"))).build());
        }
        for (JsonNode n : root.get("favorites")) {
            persist("favorites", n, Favorite.builder().id(id(n, "id"))
                    .user(users.get(id(n, "userId"))).tutor(tutors.get(id(n, "tutorId"))).build());
        }

        for (JsonNode n : root.get("chatChannels")) {
            ChatChannel e = ChatChannel.builder().id(id(n, "id")).name(s(n, "name"))
                    .lastMessageAt(instant(n, "lastMessageAt"))
                    .sender(users.get(id(n, "senderId"))).receiver(users.get(id(n, "receiverId"))).build();
            channels.put(e.getId(), persist("chat_channels", n, e));
        }
        em.flush();
        for (JsonNode n : root.get("messages")) {
            Message e = Message.builder().id(id(n, "id")).content(s(n, "content")).isRead(n.get("isRead").asBoolean())
                    .sentAt(instant(n, "sentAt")).chatChannel(channels.get(id(n, "chatChannelId")))
                    .sender(users.get(id(n, "senderId"))).build();
            messages.put(e.getId(), persist("messages", n, e));
        }
        em.flush();
        for (JsonNode n : root.get("media")) {
            persist("media", n, Media.builder().id(id(n, "id")).publicId(s(n, "publicId")).url(s(n, "url"))
                    .type(MediaType.valueOf(s(n, "type"))).message(messages.get(id(n, "messageId")))
                    .user(users.get(id(n, "userId"))).build());
        }
        for (JsonNode n : root.get("notifications")) {
            persist("notifications", n, Notification.builder().id(id(n, "id")).title(s(n, "title")).content(s(n, "content"))
                    .notificationType(NotificationType.valueOf(s(n, "notificationType"))).isRead(n.get("isRead").asBoolean())
                    .recipient(users.get(id(n, "recipientId"))).build());
        }
        for (JsonNode n : root.get("revenueReports")) {
            persist("revenue_reports", n, RevenueReport.builder().id(id(n, "id"))
                    .periodStart(date(n, "periodStart")).periodEnd(date(n, "periodEnd"))
                    .totalRevenue(n.get("totalRevenue").asDouble()).totalTransactions(n.get("totalTransactions").asInt())
                    .generatedByAdmin(admins.get(id(n, "generatedByAdminId"))).build());
        }
        em.flush();
    }

    // ------------------------------------------------------------------ timestamps
    private void applyTimestamps() {
        stamps.forEach((table, rows) -> {
            String sql = "UPDATE " + table + " SET created_at = ?, updated_at = ? WHERE id = ?";
            jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Object[] r = rows.get(i);
                    ps.setObject(1, OffsetDateTime.ofInstant((Instant) r[1], ZoneOffset.UTC));
                    ps.setObject(2, OffsetDateTime.ofInstant((Instant) r[2], ZoneOffset.UTC));
                    ps.setObject(3, r[0]);
                }

                @Override
                public int getBatchSize() {
                    return rows.size();
                }
            });
        });
    }

    // ------------------------------------------------------------------ helpers
    private <T> T persist(String table, JsonNode n, T entity) {
        em.persist(entity);
        stamps.computeIfAbsent(table, k -> new ArrayList<>())
                .add(new Object[]{id(n, "id"), instant(n, "createdAt"), instant(n, "updatedAt")});
        return entity;
    }

    private static boolean absent(JsonNode n, String f) {
        return n == null || !n.has(f) || n.get(f).isNull();
    }

    private static String s(JsonNode n, String f) {
        return absent(n, f) ? null : n.get(f).asText();
    }

    private static UUID id(JsonNode n, String f) {
        return absent(n, f) ? null : UUID.fromString(n.get(f).asText());
    }

    private static Instant instant(JsonNode n, String f) {
        return absent(n, f) ? null : Instant.parse(n.get(f).asText());
    }

    private static LocalDate date(JsonNode n, String f) {
        return absent(n, f) ? null : LocalDate.parse(n.get(f).asText());
    }

    private static BigDecimal dec(JsonNode n, String f) {
        return absent(n, f) ? null : n.get(f).decimalValue();
    }

    private static Address address(JsonNode a) {
        if (a == null || a.isNull()) {
            return null;
        }
        return Address.builder().street(s(a, "street")).ward(s(a, "ward")).province(s(a, "province")).build();
    }
}