package org.group3.tutorlink.features.chatbot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
//Lớp ChatbotConfig được sử dụng để cấu hình các thuộc tính liên quan đến chatbot trong ứng dụng.
// đánh dấu với annotatio để Spring Boot có thể nhận diện và quản lý các thuộc tính cấu hình từ lớp ChatbotProperties.
@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotConfig {
}