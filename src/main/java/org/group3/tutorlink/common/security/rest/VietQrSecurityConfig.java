package org.group3.tutorlink.common.security.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Chuỗi bảo mật riêng cho API VietQR gọi vào (/vqr/**).
 *
 * Vì sao phải tách: chuỗi chính bật oauth2ResourceServer nên mọi Bearer token đều bị giải mã bằng
 * CustomJwtDecoder (kể cả trên endpoint permitAll). Token của VietQR ký bằng khóa khác nên sẽ bị 401
 * trước khi tới controller. Chuỗi này không dùng resource server; controller tự xác thực
 * (Basic ở token_generate, Bearer của VietQR ở transaction-sync).
 */
@Configuration
@ConditionalOnProperty(name = "payment.vietqr.enabled", havingValue = "true")
public class VietQrSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain vietQrFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/vqr/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}