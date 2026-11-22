package org.group3.tutorlink.features.application.matching;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class AreaMatcher {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    /** Thiếu dữ liệu thì coi là trung tính (trả true), không phạt. */
    public boolean matches(Tutor tutor, Post post) {
        if (!requiresPhysicalPresence(tutor, post)) {
            return true;
        }

        List<String> tutorAreas = normalizeAll(tutor.getTeachingAreas());
        List<String> postPlaces = normalizeAll(placesOf(post.getAddress()));
        if (tutorAreas.isEmpty() || postPlaces.isEmpty()) {
            return true;
        }

        return tutorAreas.stream()
                .anyMatch(area -> postPlaces.stream().anyMatch(place -> overlaps(area, place)));
    }

    // Chỉ xét khi học viên cần học trực tiếp; gia sư chỉ dạy online đã bị tính ở cảnh báo hình thức dạy
    private boolean requiresPhysicalPresence(Tutor tutor, Post post) {
        return post.getTeachingMode() == TeachingMode.OFFLINE
                && tutor.getTeachingMode() != TeachingMode.ONLINE;
    }

    // Không dùng street: quá cụ thể để so với khu vực gia sư nhận dạy
    private List<String> placesOf(Address address) {
        if (address == null) {
            return List.of();
        }
        return Stream.of(address.getWard(), address.getProvince()).toList();
    }

    private boolean overlaps(String area, String place) {
        return area.contains(place) || place.contains(area);
    }

    private List<String> normalizeAll(Collection<String> texts) {
        if (texts == null) {
            return List.of();
        }
        return texts.stream()
                .filter(text -> text != null && !text.isBlank())
                .map(this::normalize)
                .toList();
    }

    // Bỏ dấu, đ -> d, chữ thường: "Thành phố Hồ Chí Minh" và "thanh pho ho chi minh" coi như nhau
    private String normalize(String text) {
        String withoutMarks = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return withoutMarks.replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}