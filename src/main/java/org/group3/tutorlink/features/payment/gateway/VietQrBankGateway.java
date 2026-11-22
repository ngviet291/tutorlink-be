package org.group3.tutorlink.features.payment.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.bank.BankGateway;
import org.group3.tutorlink.features.payment.gateway.bank.BankInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Gọi GET https://api.vietqr.io/v2/banks (API công khai, không cần token).
 * Phản hồi dạng: { "code": "00", "desc": "...", "data": [ { id, name, code, bin, shortName, logo,
 * transferSupported, lookupSupported, ... } ] }.
 * Lớp này chỉ lo gọi HTTP và đọc JSON; cache và định dạng trả về cho API nằm ở BankService.
 */
@Slf4j
@Component
public class VietQrBankGateway implements BankGateway {

    private static final String SUCCESS_CODE = "00";
    private static final String USER_AGENT = "TutorLink/1.0";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient client;
    private final String banksUrl;

    public VietQrBankGateway(
            @Value("${payment.vietqr.banks-url}") String banksUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.client = RestClient.builder().requestFactory(factory).build();
        this.banksUrl = banksUrl;
    }

    @Override
    public List<BankInfo> fetchBanks() {
        try {
            String body = client.get()
                    .uri(banksUrl)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(String.class);
            return parse(body);
        } catch (RestClientException | JsonProcessingException ex) {
            log.warn("VietQR banks request failed: {}", ex.getClass().getSimpleName());
            throw new AppException(PaymentErrorCode.BANK_LIST_UNAVAILABLE);
        }
    }

    private List<BankInfo> parse(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) {
            throw new AppException(PaymentErrorCode.BANK_LIST_UNAVAILABLE);
        }
        JsonNode root = MAPPER.readTree(body);
        JsonNode data = root.path("data");
        if (!SUCCESS_CODE.equals(root.path("code").asText()) || !data.isArray()) {
            log.warn("VietQR banks unexpected response, code={}", root.path("code").asText());
            throw new AppException(PaymentErrorCode.BANK_LIST_UNAVAILABLE);
        }
        List<BankInfo> banks = new ArrayList<>(data.size());
        for (JsonNode node : data) {
            String code = node.path("code").asText("");
            if (code.isBlank()) {
                continue;
            }
            banks.add(new BankInfo(
                    node.hasNonNull("id") ? node.get("id").asInt() : null,
                    code,
                    node.path("bin").asText(""),
                    node.path("name").asText(""),
                    node.path("shortName").asText(""),
                    node.path("logo").asText(""),
                    node.path("transferSupported").asInt(0) == 1,
                    node.path("lookupSupported").asInt(0) == 1));
        }
        return banks;
    }
}