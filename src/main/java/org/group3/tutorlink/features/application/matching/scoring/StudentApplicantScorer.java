package org.group3.tutorlink.features.application.matching.scoring;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.application.matching.AreaMatcher;
import org.group3.tutorlink.features.application.matching.GradeLevelResolver;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.user.entity.Student;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Chấm học viên ứng tuyển vào bài FIND_STUDENT của gia sư, cùng khung với phía học viên chấm gia sư:
 * trung bình có trọng số (tiêu chí thiếu dữ liệu thì bỏ qua) rồi nhân hệ số phạt khi lệch.
 * Tiêu chí: khu vực (khoảng cách nhà tới nơi học), cấp học, tin cậy (số lớp đã theo đến cùng),
 * mức nghiêm túc (có lời nhắn và mục tiêu học), độ hợp ngữ nghĩa với bài (vector).
 */
@Component
@RequiredArgsConstructor
public class StudentApplicantScorer {

    private final MatchingProperties properties;
    private final AreaMatcher areaMatcher;
    private final GradeLevelResolver gradeResolver;

    public ScoredCandidate score(Post post, Application application,
                                 Map<UUID, Long> completedCounts, Map<UUID, Double> semanticScores) {
        MatchingProperties.StudentApplicant cfg = properties.getStudentApplicant();
        MatchingProperties.Penalty penalty = properties.getPenalty();
        Student student = application.getStudent();

        Map<String, Double> breakdown = new LinkedHashMap<>();
        List<MatchWarning> warnings = new ArrayList<>();
        double sum = 0;
        double weights = 0;
        double factor = 1;

        Optional<AreaMatcher.AreaFit> area = areaMatcher.evaluate(student.getAddress(), post);
        if (area.isPresent()) {
            breakdown.put("area", area.get().score());
            sum += cfg.getAreaWeight() * area.get().score();
            weights += cfg.getAreaWeight();
            if (area.get().far()) {
                warnings.add(MatchWarning.AREA_MISMATCH);
                factor *= penalty.getArea();
            }
        }

        Optional<EducationLevel> level = gradeResolver.resolve(student.getGrade());
        if (level.isPresent() && post.getEducationLevel() != null) {
            boolean match = level.get() == post.getEducationLevel();
            breakdown.put("level", match ? 1.0 : 0.0);
            sum += cfg.getLevelWeight() * (match ? 1 : 0);
            weights += cfg.getLevelWeight();
            if (!match) {
                warnings.add(MatchWarning.LEVEL_MISMATCH);
                factor *= penalty.getLevel();
            }
        }

        long completed = completedCounts.getOrDefault(student.getId(), 0L);
        double trust = Math.min((double) completed / cfg.getCompletedCap(), 1.0);
        breakdown.put("trust", trust);
        sum += cfg.getTrustWeight() * trust;
        weights += cfg.getTrustWeight();

        double profile = ((StringUtils.hasText(application.getMessage()) ? 0.5 : 0)
                + (StringUtils.hasText(student.getLearningGoal()) ? 0.5 : 0));
        breakdown.put("profile", profile);
        sum += cfg.getProfileWeight() * profile;
        weights += cfg.getProfileWeight();

        // Học viên chưa được index (hồ sơ trống) hoặc vector lỗi: bỏ qua tiêu chí này
        Double semantic = semanticScores.get(student.getId());
        if (semantic != null) {
            breakdown.put("semantic", semantic);
            sum += cfg.getSemanticWeight() * semantic;
            weights += cfg.getSemanticWeight();
        }

        double base = weights == 0 ? 0 : sum / weights;
        return new ScoredCandidate(application, base * factor, breakdown, warnings);
    }
}