package org.group3.tutorlink.features.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final String NEW_PASSWORD_PLACEHOLDER = "{{newPassword}}";
    private static final String TITLE_PLACEHOLDER = "{{title}}";
    private static final String CONTENT_PLACEHOLDER = "{{content}}";
    private final JavaMailSender mailSender;
    private final String resetPasswordSubject;
    private final String resetPasswordTemplate;
    private final String notificationSubjectFormat;
    private final String notificationTemplate;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.reset-password.subject}") String resetPasswordSubject,
            @Value("${app.mail.reset-password.template}") String resetPasswordTemplate,
            @Value("${app.mail.notification.subject-format}") String notificationSubjectFormat,
            @Value("${app.mail.notification.template}") String notificationTemplate) {
        this.mailSender = mailSender;
        this.resetPasswordSubject = resetPasswordSubject;
        this.resetPasswordTemplate = resetPasswordTemplate;
        this.notificationSubjectFormat = notificationSubjectFormat;
        this.notificationTemplate = notificationTemplate;
    }

    public void sendResetPasswordEmail(String to, String newPassword) {
        try {
            String html = readTemplate(resetPasswordTemplate)
                    .replace(NEW_PASSWORD_PLACEHOLDER, escapeHtml(newPassword));
            sendHtml(to, resetPasswordSubject, html);
        } catch (IOException | MessagingException e) {
            throw new IllegalStateException("Failed to send password reset email", e);
        }
    }

    public void sendNotificationEmail(String to, String title, String content) {
        try {
            String html = readTemplate(notificationTemplate)
                    .replace(TITLE_PLACEHOLDER, escapeHtml(title))
                    .replace(CONTENT_PLACEHOLDER, escapeHtml(content));
            sendHtml(to, String.format(notificationSubjectFormat, title), html);
        } catch (IOException | MessagingException e) {
            throw new IllegalStateException("Failed to send notification email", e);
        }
    }

    private String readTemplate(String path) throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource(path).getInputStream(),
                StandardCharsets.UTF_8
        );
    }

    private void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}