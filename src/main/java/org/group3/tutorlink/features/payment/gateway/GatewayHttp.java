package org.group3.tutorlink.features.payment.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

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
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory).build();
    }

    public static Map<String, Object> postJson(String url, Map<String, Object> body) {
        try {
            String response = CLIENT.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "TutorLink/1.0")
                    .body(MAPPER.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw requestFailure(ex);
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
            throw requestFailure(ex);
        }
    }

    public enum Failure {
        TIMEOUT, DNS, TLS, CONNECTION, TRANSPORT, HTTP_REJECTED,
        REQUEST_ENCODING, EMPTY_RESPONSE, INVALID_JSON, INVALID_RESPONSE,
        MOMO_REJECTED, MISSING_PAYMENT_LINK
    }

    // Only controlled codes and numeric metadata reach the API/logs.
    public static AppException diagnostic(Failure failure, Integer httpStatus, Integer resultCode) {
        String message = "[PAYMENT_" + failure.name() + "]"
                + (httpStatus == null ? "" : " HTTP=" + httpStatus)
                + (resultCode == null ? "" : " resultCode=" + resultCode);
        log.warn("Payment gateway failure: {}", message);
        return new AppException(new Diagnostic(message));
    }

    private record Diagnostic(String getMessage) implements BaseErrorCode {
        @Override
        public HttpStatus getHttpStatus() { return HttpStatus.BAD_GATEWAY; }
    }

    static AppException requestFailure(Exception ex) {
        if (ex instanceof RestClientResponseException response) {
            Integer code = null;
            try {
                var root = MAPPER.readTree(response.getResponseBodyAsString());
                var value = root == null ? null : root.get("resultCode");
                if (value != null && value.isIntegralNumber() && value.canConvertToInt()) {
                    code = value.intValue();
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                // Never expose a provider body/message, including HTML error pages.
            }
            return diagnostic(Failure.HTTP_REJECTED, response.getStatusCode().value(), code);
        }
        Throwable cause = ex;
        for (int depth = 0; cause != null && depth < 16; depth++, cause = cause.getCause()) {
            Failure kind = null;
            if (cause instanceof java.net.SocketTimeoutException) kind = Failure.TIMEOUT;
            else if (cause instanceof java.net.UnknownHostException) kind = Failure.DNS;
            else if (cause instanceof javax.net.ssl.SSLException) kind = Failure.TLS;
            else if (cause instanceof java.net.ConnectException) kind = Failure.CONNECTION;
            else if (cause instanceof com.fasterxml.jackson.core.JsonProcessingException) kind = Failure.REQUEST_ENCODING;
            if (kind != null) return diagnostic(kind, null, null);
        }
        return diagnostic(Failure.TRANSPORT, null, null);
    }

    public static Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            throw diagnostic(Failure.EMPTY_RESPONSE, null, null);
        }
        try {
            var root = MAPPER.readTree(json);
            if (root == null || !root.isObject()) {
                throw diagnostic(Failure.INVALID_JSON, null, null);
            }
            return MAPPER.convertValue(root, new TypeReference<Map<String, Object>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException ex) {
            throw diagnostic(Failure.INVALID_JSON, null, null);
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
