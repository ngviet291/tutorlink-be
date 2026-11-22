package org.group3.tutorlink.features.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.PromptLoader;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.ai.dto.response.TutorMatchScore;
import org.group3.tutorlink.features.ai.exception.ErrorCodeAi;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TutorMatchingAiService {

    private static final int MIN_SCORE = 0;

    private final ChatClient chatClient;
    private final PromptLoader promptLoader;
    private final String userPromptPath;
    private final String systemPromptPath;
    private final String postBlockPath;
    private final String candidateBlockPath;
    private final int maxScore;
    private final int maxTextLength;

    public TutorMatchingAiService(
            ChatClient.Builder builder,
            PromptLoader promptLoader,
            @Value("${app.ai.prompt.path.tutor-matching}") String userPromptPath,
            @Value("${app.ai.prompt.path.tutor-matching-system}") String systemPromptPath,
            @Value("${app.ai.prompt.path.tutor-matching-post}") String postBlockPath,
            @Value("${app.ai.prompt.path.tutor-matching-candidate}") String candidateBlockPath,
            @Value("${app.ai.matching.max-score}") int maxScore,
            @Value("${app.ai.matching.max-text-length}") int maxTextLength) {
        this.chatClient = builder.build();
        this.promptLoader = promptLoader;
        this.userPromptPath = userPromptPath;
        this.systemPromptPath = systemPromptPath;
        this.postBlockPath = postBlockPath;
        this.candidateBlockPath = candidateBlockPath;
        this.maxScore = maxScore;
        this.maxTextLength = maxTextLength;
    }

    /**
     * Chấm độ phù hợp nội dung giữa bài đăng và từng gia sư ứng tuyển.
     *
     * @return map applicationId -> điểm 0..1
     * @throws AppException AI_PROVIDER_ERROR (502) khi gọi AI thất bại
     */
    public Map<UUID, Double> scoreSemanticFit(Post post, List<Application> applications) {
        String systemTemplate = promptLoader.load(systemPromptPath);
        String userTemplate = promptLoader.load(userPromptPath);
        String postBlock = describePost(post);
        String candidatesBlock = describeCandidates(applications);

        List<TutorMatchScore> result;
        try {
            result = chatClient.prompt()
                    .system(s -> s.text(systemTemplate).param("maxScore", maxScore))
                    .user(u -> u.text(userTemplate)
                            .param("post", postBlock)
                            .param("candidates", candidatesBlock))
                    .call()
                    .entity(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("Tutor matching failed", e);
            throw new AppException(ErrorCodeAi.AI_PROVIDER_ERROR);
        }

        if (result == null) {
            return Map.of();
        }
        return toScoreMap(result, applications);
    }

    // ---------- Chuyển kết quả AI thành map điểm ----------

    private Map<UUID, Double> toScoreMap(List<TutorMatchScore> scores, List<Application> applications) {
        Set<UUID> validIds = applications.stream()
                .map(Application::getId)
                .collect(Collectors.toSet());

        return scores.stream()
                .filter(s -> s.getScore() != null)
                .flatMap(s -> parseId(s.getApplicationId())
                        .filter(validIds::contains)                  // bỏ id AI tự bịa
                        .map(id -> Map.entry(id, normalize(s.getScore())))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));
    }

    private Optional<UUID> parseId(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private double normalize(int score) {
        return Math.min(maxScore, Math.max(MIN_SCORE, score)) / (double) maxScore;
    }

    // ---------- Dựng dữ liệu đưa vào prompt (không gửi tên, email, SĐT, địa chỉ) ----------

    private String describePost(Post post) {
        PromptTemplate template = new PromptTemplate(promptLoader.load(postBlockPath));
        return template.render(Map.<String, Object>of(
                "title", sanitize(post.getTitle()),
                "content", sanitize(post.getContent()),
                "subject", post.getSubject() == null ? "" : sanitize(post.getSubject().getName()),
                "level", Objects.toString(post.getEducationLevel(), "")));
    }

    private String describeCandidates(List<Application> applications) {
        PromptTemplate template = new PromptTemplate(promptLoader.load(candidateBlockPath));
        return applications.stream()
                .map(application -> template.render(candidateModel(application)))
                .collect(Collectors.joining("\n"));
    }

    private Map<String, Object> candidateModel(Application application) {
        Tutor tutor = application.getTutor();
        return Map.of(
                "id", application.getId().toString(),
                "subjects", subjectNames(tutor),
                "experienceYears", String.valueOf(tutor.getExperienceYears()),
                "education", sanitize(tutor.getEducation()),
                "bio", sanitize(tutor.getBio()),
                "message", sanitize(application.getMessage()));
    }

    private String subjectNames(Tutor tutor) {
        if (tutor.getSubjects() == null) {
            return "";
        }
        return tutor.getSubjects().stream()
                .map(Subject::getName)
                .collect(Collectors.joining(", "));
    }

    // Cắt ngắn và bỏ < > để nội dung người dùng không đóng/mở thẻ giả
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace('<', '(').replace('>', ')').strip();
        return cleaned.length() > maxTextLength ? cleaned.substring(0, maxTextLength) : cleaned;
    }
}