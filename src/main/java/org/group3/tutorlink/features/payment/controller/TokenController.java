package org.group3.tutorlink.features.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/vqr/api")
public class TokenController {

    private static final String VALID_USERNAME = "customer-vietqrtest-user2468";
    private static final String VALID_PASSWORD = "Y3VzdG9tZXItdmlldHFydGVzdC11c2VyMjQ2ODpZM1Z6ZEc5dFpYSXRkbWxsZEhGeWRHVnpkQzExYzJWeU1qUTJPQT09"; // Đây là chuỗi base64 từ username:password thật của bạn.

    // API để tạo token
    @PostMapping("/token_generate")
    public ResponseEntity<?> generateToken(@RequestHeader("Authorization") String authHeader) {
        // Kiểm tra xem header có Authorization không
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // Giải mã Base64 từ Authorization header
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

            // Phân tách username và password
            final String[] values = credentials.split(":", 2);
            String username = values[0];
            String password = values[1];

            // Kiểm tra tính hợp lệ của username và password
            if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                // Nếu hợp lệ, tạo JWT token
                String token = "your-generated-jwt-token"; // Ở đây bạn cần tạo JWT token thực sự, ví dụ với jjwt.

                return ResponseEntity.ok(new TokenResponse(token, "Bearer", 300));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization header is missing or invalid");
        }
    }

    // Class cho response
    public static class TokenResponse {
        private String access_token;
        private String token_type;
        private int expires_in;

        public TokenResponse(String access_token, String token_type, int expires_in) {
            this.access_token = access_token;
            this.token_type = token_type;
            this.expires_in = expires_in;
        }

        // Getters và Setters
        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public String getToken_type() {
            return token_type;
        }

        public void setToken_type(String token_type) {
            this.token_type = token_type;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }
    }
}

// Đây là Sample Code mang tính chất tham khảo