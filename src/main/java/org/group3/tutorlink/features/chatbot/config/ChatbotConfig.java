package org.group3.tutorlink.features.chatbot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
//Lớp ChatbotConfig để cấu hình các thuộc tính liên quan đến chatbot
//@Configuration,@EnableConfigurationProperties để Spring Boot quản lý các thuộc tính cấu hình từ ChatbotProperties.
public class ChatbotConfig {
}
