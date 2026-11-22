package org.group3.tutorlink.features.auth.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.group3.tutorlink.features.auth.validation.annotation.ValidImageFiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ImageFilesValidator implements ConstraintValidator<ValidImageFiles, List<MultipartFile>> {

    private int maxFiles;
    private long maxSizeBytes;

    @Override
    public void initialize(ValidImageFiles a) {
        this.maxFiles = a.maxFiles();
        this.maxSizeBytes = a.maxSizeBytes();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext ctx) {
        if (files == null) return true; // không bắt buộc; muốn bắt buộc thì thêm @NotEmpty

        List<MultipartFile> actual = files.stream().filter(f -> f != null && !f.isEmpty()).toList();

        if (actual.size() > maxFiles) {
            return fail(ctx, "Tối đa " + maxFiles + " ảnh");
        }
        for (MultipartFile f : actual) {
            if (f.getSize() > maxSizeBytes) {
                return fail(ctx, "Mỗi ảnh tối đa " + (maxSizeBytes / 1024 / 1024) + "MB");
            }
            if (!isSupportedImage(f)) {
                return fail(ctx, "Chỉ nhận ảnh JPG, PNG hoặc WEBP");
            }
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext ctx, String message) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    private boolean isSupportedImage(MultipartFile file) {
        byte[] h = new byte[12];
        try (InputStream in = file.getInputStream()) {
            if (in.readNBytes(h, 0, h.length) < h.length) return false;
        } catch (IOException e) {
            return false;
        }
        boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
        boolean webp = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
        return jpeg || png || webp;
    }
}