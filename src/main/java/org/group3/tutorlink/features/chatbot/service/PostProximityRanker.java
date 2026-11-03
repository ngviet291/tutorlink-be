package org.group3.tutorlink.features.chatbot.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PostProximityRanker {

    private static final int RANK_NEAREST = 0;
    private static final int RANK_SAME_PROVINCE = 1;
    private static final int RANK_OTHER = 2;

    // Biên dịch 1 lần, không biên dịch lại mỗi lần bỏ dấu
    private static final Pattern ACCENT_MARKS = Pattern.compile("\\p{M}+");

    /** Bài gần nhất đứng trước. Cùng hạng thì giữ nguyên thứ tự cũ (mới nhất trước). */
    public List<Post> sortNearestFirst(List<Post> posts, String area, UserLocation userLocation) {
        String normalizedArea = StringUtils.hasText(area) ? removeAccents(area) : null;

        // Tính hạng đúng 1 lần cho mỗi bài rồi mới sắp xếp
        return posts.stream()
                .map(post -> new RankedPost(post, rank(post, normalizedArea, userLocation)))
                .sorted(Comparator.comparingInt(RankedPost::getRank))
                .map(RankedPost::getPost)
                .toList();
    }

    // Khu vực người dùng nói trong câu hỏi được ưu tiên hơn địa chỉ hồ sơ.
    private int rank(Post post, String normalizedArea, UserLocation userLocation) {
        if (normalizedArea != null) {
            return isInArea(post, normalizedArea) ? RANK_NEAREST : RANK_OTHER;
        }
        if (!userLocation.isKnown() || post.getAddress() == null) {
            return RANK_OTHER;
        }
        if (!isSameText(post.getAddress().getProvince(), userLocation.getProvince())) {
            return RANK_OTHER;
        }
        // Cùng tỉnh: cùng phường thì gần nhất, khác phường thì xếp giữa.
        return isSameText(post.getAddress().getWard(), userLocation.getWard()) ? RANK_NEAREST : RANK_SAME_PROVINCE;
    }

    private boolean isSameText(String a, String b) {
        return StringUtils.hasText(a) && StringUtils.hasText(b) && removeAccents(a).equals(removeAccents(b));
    }

    private boolean isInArea(Post post, String normalizedArea) {
        Address address = post.getAddress();
        String haystack = Stream.of(
                        address == null ? null : address.getStreet(),
                        address == null ? null : address.getWard(),
                        post.getTitle(),
                        post.getContent())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return removeAccents(haystack).contains(normalizedArea);
    }

    /** "Gò Vấp" == "go vap". */
    private String removeAccents(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return ACCENT_MARKS.matcher(decomposed).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    @Getter
    @RequiredArgsConstructor
    private static final class RankedPost {
        private final Post post;
        private final int rank;
    }
}