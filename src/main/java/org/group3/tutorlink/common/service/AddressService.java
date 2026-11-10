package org.group3.tutorlink.common.service;

import org.group3.tutorlink.common.client.ProvincesApiClient;
import org.group3.tutorlink.common.dto.external.ExternalProvince;
import org.group3.tutorlink.common.dto.external.ProvinceApiResponse;
import org.group3.tutorlink.common.dto.response.ProvinceResponse;
import org.group3.tutorlink.common.dto.response.WardResponse;
import org.group3.tutorlink.common.mapper.AddressMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    /**
     * Đơn vị hành chính hiếm khi đổi nên cache 24 giờ.
     */
    private static final Duration TTL = Duration.ofHours(24);
    private static final long CACHE_TTL = 24 * 60 * 60 * 1000L;

    private static final Pattern DIACRITICS =
            Pattern.compile("\\p{M}+");


    private final ProvincesApiClient client;
    private final AddressMapper mapper;

    // Tối đa 1 key danh sách tỉnh + ~34 key phường/xã; mã tỉnh sai ném lỗi nên không bao giờ vào cache.
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    public List<ProvinceResponse> getProvinces() {
        return cached("provinces", () -> mapper.toProvinces(client.fetchProvinces()));
    }

    public List<WardResponse> getWards(int provinceCode) {
        return cached("wards:" + provinceCode, () -> {
            ExternalProvince province = client.fetchProvinceWithWards(provinceCode);
            return mapper.toWards(province.getWards(), provinceCode);
        });
    }

    /**
     * Trả từ cache nếu còn hạn; hết hạn thì tải lại.
     * Nếu tải lại lỗi mà còn bản cũ thì trả bản cũ (stale-if-error) thay vì làm hỏng form đăng ký.
     */
    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> loader) {
        CacheEntry<T> entry = (CacheEntry<T>) cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        try {
            T value = loader.get();
            cache.put(key, new CacheEntry<>(value, Instant.now().plus(TTL)));
            return value;
        } catch (AppException e) {
            if (entry != null) {
                log.warn("Address API failed for '{}', serving stale cache", key);
                return entry.getValue();
            }
            throw e;
        }
    }

    public String normalize(String provinceName) {
        if (provinceName == null || provinceName.isBlank()) {
            return provinceName;
        }

        try {
            List<ProvinceApiResponse> provinceList = client.fetchProvinces().stream()
                    .map(e -> ProvinceApiResponse.builder().name(e.getName()).build()).collect(Collectors.toList());

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

    @Getter
    @RequiredArgsConstructor
    private static class CacheEntry<T> {
        private final T value;
        private final Instant expiresAt;

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}