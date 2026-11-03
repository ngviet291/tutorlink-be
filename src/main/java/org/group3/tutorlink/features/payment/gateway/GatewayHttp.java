package org.group3.tutorlink.features.payment.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tiện ích gọi HTTP + đọc JSON dùng chung cho các cổng thanh toán.
 * Body gửi/nhận dạng String và tự parse bằng ObjectMapper nên không phụ thuộc message converter của Spring.
 */
@Slf4j
public final class GatewayHttp {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final RestClient CLIENT = buildClient();

    private GatewayHttp() {
    }

    private static RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(factory).build();
    }

    public static Map<String, Object> postJson(String url, Map<String, Object> body) {
        try {
            String response = CLIENT.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MAPPER.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("Gateway request failed: {}", url, ex);
            throw new AppException(PaymentErrorCode.GATEWAY_ERROR);
        }
    }

    public static Map<String, Object> postForm(String url, Map<String, String> form) {
        String encoded = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        try {
            String response = CLIENT.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(encoded)
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (RestClientException ex) {
            log.error("Gateway request failed: {}", url, ex);
            throw new AppException(PaymentErrorCode.GATEWAY_ERROR);
        }
    }

    // Parse JSON object -> Map; body rỗng/sai cú pháp -> GATEWAY_ERROR
    public static Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            throw new AppException(PaymentErrorCode.GATEWAY_ERROR);
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.warn("Cannot parse gateway JSON: {}", ex.getMessage());
            throw new AppException(PaymentErrorCode.GATEWAY_ERROR);
        }
    }

    public static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public static Integer intValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.intValue() : null;
    }

    public static BigDecimal decimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
