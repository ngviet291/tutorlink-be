package org.group3.tutorlink.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.geo.GeoPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Tra tọa độ trung tâm của phường/xã (và tỉnh) từ file JSON nạp một lần khi khởi động.
 * Dữ liệu: 3.321 phường/xã của 34 tỉnh/thành (cấu trúc hành chính từ 2025).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WardLocationService {

    private static final Pattern PROVINCE_PREFIX = Pattern.compile("^(thanh pho|tinh|tp\\.?)\\s*");
    private static final Pattern WARD_PREFIX = Pattern.compile("^(phuong|xa|thi tran|dac khu)\\s+");

    private final LocationNormalizationService normalizer;

    @Value("${app.address.ward-locations:classpath:data/ward_locations.json}")
    private Resource source;

    // key = "tỉnh|phường/xã đầy đủ" (đã chuẩn hóa) và "tỉnh|phường/xã không tiền tố"
    private final Map<String, GeoPoint> wardsExact = new HashMap<>();
    private final Map<String, GeoPoint> wardsLoose = new HashMap<>();
    private final Map<String, GeoPoint> provinces = new HashMap<>();

    @PostConstruct
    void load() {
        try (InputStream in = source.getInputStream()) {
            // [{code, name, lat, lon, wards: [{code, name, lat, lon}]}]
            for (JsonNode p : new ObjectMapper().readTree(in)) {
                String province = province(p.get("name").asText());
                provinces.putIfAbsent(province, point(p));

                for (JsonNode w : p.get("wards")) {
                    String ward = normalizer.normalizeLocationText(w.get("name").asText());
                    GeoPoint wardPoint = point(w);
                    wardsExact.putIfAbsent(province + "|" + ward, wardPoint);
                    // Tên trùng trong cùng tỉnh (hiếm) thì lấy phường/xã đầu tiên
                    wardsLoose.putIfAbsent(province + "|" + stripWardPrefix(ward), wardPoint);
                }
            }
            log.info("Đã nạp tọa độ {} phường/xã của {} tỉnh/thành", wardsExact.size(), provinces.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được dữ liệu tọa độ phường/xã", e);
        }
    }

    private GeoPoint point(JsonNode node) {
        return new GeoPoint(node.get("lat").asDouble(), node.get("lon").asDouble());
    }

    /** Tọa độ phường/xã; nhận cả "Phường Tân Phong", "P. Tân Phong" hay "Tân Phong". */
    public Optional<GeoPoint> findWard(String provinceName, String wardName) {
        if (!StringUtils.hasText(provinceName) || !StringUtils.hasText(wardName)) {
            return Optional.empty();
        }
        String province = province(provinceName);
        String ward = normalizer.normalizeLocationText(wardName);
        GeoPoint exact = wardsExact.get(province + "|" + ward);
        if (exact != null) {
            return Optional.of(exact);
        }
        return Optional.ofNullable(wardsLoose.get(province + "|"
                + stripWardPrefix(normalizer.normalizeWardForSearch(wardName))));
    }

    public Optional<GeoPoint> findProvince(String provinceName) {
        return StringUtils.hasText(provinceName)
                ? Optional.ofNullable(provinces.get(province(provinceName)))
                : Optional.empty();
    }

    private String province(String name) {
        String text = normalizer.normalizeLocationText(name);
        return text == null ? "" : PROVINCE_PREFIX.matcher(text).replaceFirst("").trim();
    }

    private String stripWardPrefix(String normalizedWard) {
        return normalizedWard == null ? "" : WARD_PREFIX.matcher(normalizedWard).replaceFirst("").trim();
    }
}
