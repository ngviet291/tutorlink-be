package org.group3.tutorlink.features.chatbot.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.group3.tutorlink.features.post.enums.BudgetUnit;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

//Thông tin tóm tắt của bài đăng cho chatbot. KHÔNG có SĐT/email (chỉ mở sau khi thanh toán phí nhận lớp).
@Getter
@Builder
public class PostSummaryResponse {
    private UUID id;
    private String title;
    private String subjectName;
    private String location;
    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    private BudgetUnit budgetUnit;
    private String authorName;
    private String authorAvatarUrl;
}