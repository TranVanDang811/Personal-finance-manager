package com.pfm.personalfinancemanager.util;

import com.pfm.personalfinancemanager.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class ImageFileValidator {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private ImageFileValidator() {
    }

    public static String validateAndDetectExtension(MultipartFile file, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn file ảnh hợp lệ");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(String.format(
                    "Kích thước ảnh vượt quá giới hạn cho phép (%d MB)",
                    Math.max(1L, maxSizeBytes / (1024 * 1024))
            ));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.containsKey(contentType.toLowerCase())) {
            throw new BusinessException("Chỉ hỗ trợ định dạng ảnh JPG, PNG hoặc WEBP");
        }

        String extension = detectImageExtensionByMagicBytes(file);
        if (extension == null) {
            throw new BusinessException("Nội dung file không phải là ảnh hợp lệ (JPG, PNG, WEBP)");
        }
        return extension;
    }

    public static String resolveMimeType(String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static String detectImageExtensionByMagicBytes(MultipartFile file) {
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                return null;
            }
            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (bytesRead >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50
                    && header[2] == 0x4E && header[3] == 0x47) {
                return "png";
            }
            // WEBP: RIFF....WEBP
            if (bytesRead >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "webp";
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
}

