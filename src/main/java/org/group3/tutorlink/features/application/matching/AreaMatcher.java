package org.group3.tutorlink.features.application.matching;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.geo.GeoPoint;
import org.group3.tutorlink.common.geo.GeoUtils;
import org.group3.tutorlink.common.geo.ProximityScorer;
import org.group3.tutorlink.common.service.LocationNormalizationService;
import org.group3.tutorlink.common.service.WardLocationService;
import org.group3.tutorlink.features.chatbot.ranking.SearchAliasService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Đánh giá khu vực gia sư nhận dạy so với địa chỉ bài đăng, cùng cách với chatbot:
 * tra tọa độ tâm phường/xã rồi tính khoảng cách Haversine, gần hơn thì điểm cao hơn.
 *
 * Mỗi mục trong teachingAreas (chuỗi tự do) được hiểu là phường/xã hoặc tỉnh/thành;
 * lấy mục cho điểm cao nhất. Mục không nhận ra được thì bỏ qua, không phạt.
 */
@Component
@RequiredArgsConstructor
public class AreaMatcher {

    private final WardLocationService wardLocations;
    private final LocationNormalizationService normalizer;
    private final SearchAliasService aliases;
    private final MatchingProperties properties;

    /**
     * @param score điểm khu vực 0..1
     * @param far   true khi mọi khu vực nhận ra được đều quá xa hoặc khác tỉnh
     */
    public record AreaFit(double score, boolean far) {
    }

    /** Rỗng = không đủ dữ liệu để đánh giá (trung tính, không phạt). */
    public Optional<AreaFit> evaluate(Tutor tutor, Post post) {
        if (!requiresPhysicalPresence(tutor, post)) {
            return Optional.empty();
        }
        List<String> areas = tutor.getTeachingAreas();
        Address address = post.getAddress();
        if (areas == null || areas.isEmpty() || address == null || !StringUtils.hasText(address.getProvince())) {
            return Optional.empty();
        }

        MatchingProperties.Area config = properties.getArea();
        GeoPoint reference = wardLocations.findWard(address.getProvince(), address.getWard()).orElse(null);

        double best = -1;
        boolean allFar = true;
        for (String area : areas) {
            Optional<AreaFit> fit = fitOf(area, address, reference, config);
            if (fit.isPresent()) {
                best = Math.max(best, fit.get().score());
                allFar &= fit.get().far();
            }
        }
        return best < 0 ? Optional.empty() : Optional.of(new AreaFit(best, allFar));
    }

    /** Thiếu dữ liệu thì coi là trung tính (trả true), không phạt. */
    public boolean matches(Tutor tutor, Post post) {
        return evaluate(tutor, post).map(fit -> !fit.far()).orElse(true);
    }

    private Optional<AreaFit> fitOf(String area, Address address, GeoPoint reference, MatchingProperties.Area config) {
        if (!StringUtils.hasText(area)) {
            return Optional.empty();
        }
        // 1. Phường/xã (trong tỉnh của bài đăng)
        Optional<GeoPoint> ward = wardLocations.findWard(address.getProvince(), area);
        if (ward.isPresent() && reference != null) {
            double km = GeoUtils.distanceKm(reference, ward.get());
            return Optional.of(new AreaFit(ProximityScorer.score(km, config.getRadiusKm()), km > config.getFarKm()));
        }
        // Không có tọa độ phường của bài: quay về so tên
        if (ward.isPresent() || normalizer.isSameWard(area, address.getWard())) {
            boolean same = normalizer.isSameWard(area, address.getWard());
            return Optional.of(new AreaFit(same ? 1.0 : 0.0, false));
        }
        // 2. Tỉnh/thành: "Hanoi", "Saigon", "Thành phố Hồ Chí Minh"...
        Optional<GeoPoint> province = wardLocations.findProvince(aliases.province(area));
        if (province.isPresent()) {
            return sameProvince(province.get(), address.getProvince())
                    ? Optional.of(new AreaFit(config.getSameProvinceScore(), false))
                    : Optional.of(new AreaFit(0.0, true));
        }
        return Optional.empty();
    }

    private boolean sameProvince(GeoPoint areaProvince, String postProvince) {
        return wardLocations.findProvince(aliases.province(postProvince))
                .map(p -> p.getLatitude() == areaProvince.getLatitude()
                        && p.getLongitude() == areaProvince.getLongitude())
                .orElse(false);
    }

    // Chỉ xét khi học viên cần học trực tiếp; gia sư chỉ dạy online đã bị tính ở cảnh báo hình thức dạy
    private boolean requiresPhysicalPresence(Tutor tutor, Post post) {
        return post.getTeachingMode() == TeachingMode.OFFLINE
                && tutor.getTeachingMode() != TeachingMode.ONLINE;
    }
}