package org.group3.tutorlink.common.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class LocationNormalizationService {

    private static final Pattern ACCENT_MARKS = Pattern.compile("\\p{M}+");

    // Chạy SAU khi đã bỏ dấu + viết thường, nên phải viết không dấu: "phuong", "xa", "thi tran"...
    // Khớp: "phuong tan phong", "xa my hoa", "p. tan phong", "p.12", "p 12".
    private static final Pattern WARD_PREFIX = Pattern.compile(
            "^(?:(?:phuong|xa|thi tran|quan|huyen)\\s+|(?:p|x|tt|q|h)\\.\\s*|(?:p|q)\\s+(?=\\d))"
    );

    /**
     * Chuẩn hóa tên phường/xã để so sánh: bỏ dấu, bỏ chữ Phường/Xã/Thị trấn..., viết thường.
     *
     * "Tân Phong" -> "tan phong"
     * "Phường Tân Phong" -> "tan phong"
     * "P. Tân Phong" -> "tan phong"
     * "Phường 12" -> "12"
     */
    public String normalizeWardForSearch(String value) {
        String text = normalizeLocationText(value);
        return text == null ? null : WARD_PREFIX.matcher(text).replaceFirst("").trim();
    }

    /** Hai tên phường/xã có cùng 1 nơi không, bất kể dấu, hoa thường hay chữ "Phường/Xã". */
    public boolean isSameWard(String a, String b) {
        String first = normalizeWardForSearch(a);
        return first != null && first.equals(normalizeWardForSearch(b));
    }

    public String normalizeLocationText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return removeAccents(value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String removeAccents(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return ACCENT_MARKS.matcher(decomposed)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }
}