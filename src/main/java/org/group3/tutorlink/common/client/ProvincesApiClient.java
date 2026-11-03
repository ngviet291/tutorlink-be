package org.group3.tutorlink.common.client;

import org.group3.tutorlink.common.dto.external.ExternalProvince;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/** Gọi API ngoài provinces.open-api.vn (v2). Chỉ chịu trách nhiệm HTTP, không cache. */
@Slf4j
@Component
public class ProvincesApiClient {

    private final RestClient restClient;

    public ProvincesApiClient(
            @Value("${app.address.provinces-api.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** GET /p/ — danh sách tỉnh (trường wards rỗng). */
    public List<ExternalProvince> fetchProvinces() {
        try {
            ExternalProvince[] body = restClient.get()
                    .uri("/p/")
                    .retrieve()
                    .body(ExternalProvince[].class);
            return body == null ? List.of() : Arrays.asList(body);
        } catch (RestClientException e) {
            log.error("Provinces API failed: GET /p/", e);
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /** GET /p/{code}?depth=2 — một tỉnh kèm danh sách phường/xã. */
    public ExternalProvince fetchProvinceWithWards(int provinceCode) {
        try {
            ExternalProvince body = restClient.get()
                    .uri("/p/{code}?depth=2", provinceCode)
                    .retrieve()
                    .body(ExternalProvince.class);
            if (body == null) {
                throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            return body;
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppException(ErrorCode.PROVINCE_NOT_FOUND);
        } catch (RestClientException e) {
            log.error("Provinces API failed: GET /p/{}?depth=2", provinceCode, e);
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }
}