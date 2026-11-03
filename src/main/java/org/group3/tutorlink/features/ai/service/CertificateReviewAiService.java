package org.group3.tutorlink.features.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.PromptLoader;
import org.group3.tutorlink.features.ai.exception.AiProviderException;
import org.group3.tutorlink.features.auth.dto.CertificateCheck;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;

@Slf4j
@Service
public class CertificateReviewAiService {

    private final ChatClient chatClient;
    private final PromptLoader promptLoader;
    private final String systemPromptPath;
    private final String userPromptPath;
    private final int maxTextLength;

    public CertificateReviewAiService(
            ChatClient.Builder builder,
            PromptLoader promptLoader,
            @Value("${app.ai.prompt.path.certificate-review-system}") String systemPromptPath,
            @Value("${app.ai.prompt.path.certificate-review-user}") String userPromptPath,
            @Value("${app.ai.certificate.max-text-length}") int maxTextLength) {
        this.chatClient = builder.build();
        this.promptLoader = promptLoader;
        this.systemPromptPath = systemPromptPath;
        this.userPromptPath = userPromptPath;
        this.maxTextLength = maxTextLength;
    }

    /**
     * Kiểm tra một ảnh chứng chỉ và trích xuất thông tin.
     *
     * @throws AiProviderException khi gọi AI thất bại hoặc AI không trả kết quả
     */
    public CertificateCheck check(String fullName, String imageUrl) {
        String systemTemplate = promptLoader.load(systemPromptPath);
        String userTemplate = promptLoader.load(userPromptPath);

        CertificateCheck result;
        try {
            result = chatClient.prompt()
                    .system(s -> s.text(systemTemplate))
                    .user(u -> u.text(userTemplate)
                            .param("fullName", sanitize(fullName))
                            .media(mimeOf(imageUrl), UrlResource.from(URI.create(imageUrl))))
                    .call()
                    .entity(CertificateCheck.class);
        } catch (Exception e) {
            log.error("Certificate review failed", e);
            throw new AiProviderException();
        }

        if (result == null) {
            throw new AiProviderException();
        }
        return result;
    }

    private MimeType mimeOf(String url) {
        String u = url.toLowerCase();
        if (u.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (u.endsWith(".webp")) return MimeType.valueOf("image/webp");
        return MimeTypeUtils.IMAGE_JPEG;
    }

    // Cắt ngắn và bỏ < > để tên người dùng không đóng/mở thẻ giả trong prompt
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace('<', '(').replace('>', ')').strip();
        return cleaned.length() > maxTextLength ? cleaned.substring(0, maxTextLength) : cleaned;
    }
}