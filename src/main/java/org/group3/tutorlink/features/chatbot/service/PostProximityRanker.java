package org.group3.tutorlink.features.chatbot.service;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PostProximityRanker {

    private static final int RANK_NEAREST = 0;
    private static final int RANK_SAME_PROVINCE = 1;
    private static final int RANK_OTHER = 2;

    //Bài gần nhất đứng trước. Cùng hạng thì giữ nguyên thứ tự cũ (mới nhất trước).
    public Comparator<Post> nearestFirst(String area, UserLocation userLocation) {
        return Comparator.comparingInt(post -> rank(post, area, userLocation));
    }

    // Khu vực người dùng nói trong câu hỏi được ưu tiên hơn địa chỉ hồ sơ.
    private int rank(Post post, String area, UserLocation userLocation) {
        if (StringUtils.hasText(area)) {
            return isInArea(post, area) ? RANK_NEAREST : RANK_OTHER;
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

    private boolean isInArea(Post post, String area) {
        Address address = post.getAddress();
        String haystack = Stream.of(
                        address == null ? null : address.getStreet(),
                        address == null ? null : address.getWard(),
                        post.getTitle(),
                        post.getContent())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return removeAccents(haystack).contains(removeAccents(area));
    }

    /** "Gò Vấp" == "go vap". */
    private String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
