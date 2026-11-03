package org.group3.tutorlink.common.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.ProvinceApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProvinceNormalizationService {

    private static final long CACHE_TTL = 24 * 60 * 60 * 1000L;

    private static final Pattern DIACRITICS =
            Pattern.compile("\\p{M}+");

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.address.provinces-api-url}")
    private String apiUrl;

    private volatile List<ProvinceApiResponse> provinces = List.of();

    private volatile long lastFetchTime;

    public String normalize(String provinceName) {
        if (provinceName == null || provinceName.isBlank()) {
            return provinceName;
        }

        try {
            List<ProvinceApiResponse> provinceList = getProvinces();

            String input = normalizeText(provinceName);

            return provinceList.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> p.getName() != null)
                    .filter(p -> normalizeText(p.getName()).equals(input))
                    .map(ProvinceApiResponse::getName)
                    .findFirst()
                    .orElse(provinceName.trim());

        } catch (Exception e) {
            log.warn("Không thể chuẩn hóa tỉnh/thành phố '{}': {}",
                    provinceName, e.getMessage());

            return provinceName.trim();
        }
    }

    private List<ProvinceApiResponse> getProvinces() {
        long now = System.currentTimeMillis();

        if (!provinces.isEmpty() && now - lastFetchTime < CACHE_TTL) {
            return provinces;
        }

        synchronized (this) {
            now = System.currentTimeMillis();

            if (!provinces.isEmpty() && now - lastFetchTime < CACHE_TTL) {
                return provinces;
            }

            ProvinceApiResponse[] response =
                    restTemplate.getForObject(apiUrl, ProvinceApiResponse[].class);

            if (response == null || response.length == 0) {
                throw new IllegalStateException("API không trả về danh sách tỉnh/thành phố");
            }

            provinces = List.of(response);
            lastFetchTime = now;

            log.info("Đã tải {} tỉnh/thành phố từ API", provinces.size());

            return provinces;
        }
    }

    private String normalizeText(String value) {
        String normalized = Normalizer.normalize(
                value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        normalized = DIACRITICS.matcher(normalized).replaceAll("");

        // Chuẩn hóa cách viết tiền tố hành chính, không chứa tên tỉnh cụ thể.
        normalized = normalized
                .replaceAll("^(thanh pho|tinh|tp\\.?|city of)\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized;
    }
}