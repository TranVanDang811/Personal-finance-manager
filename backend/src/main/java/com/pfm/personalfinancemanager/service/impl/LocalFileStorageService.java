package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.service.FileStorageService;
import com.pfm.personalfinancemanager.util.ImageFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final String PUBLIC_PREFIX = "/uploads/";

    private final Path rootUploadPath;

    public LocalFileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.rootUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeImage(MultipartFile file, String subDirectory, String ownerPrefix, long maxSizeBytes) {
        String extension = ImageFileValidator.validateAndDetectExtension(file, maxSizeBytes);

        try {
            Path targetDir = rootUploadPath.resolve(subDirectory).normalize();
            if (!targetDir.startsWith(rootUploadPath)) {
                throw new BusinessException("Đường dẫn lưu trữ không hợp lệ");
            }
            Files.createDirectories(targetDir);

            String safeFileName = ownerPrefix + "_" + UUID.randomUUID() + "." + extension;
            Path targetFile = targetDir.resolve(safeFileName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new BusinessException("Tên file không hợp lệ");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return PUBLIC_PREFIX + subDirectory + "/" + safeFileName;
        } catch (IOException ex) {
            log.error("Failed to store uploaded file", ex);
            throw new BusinessException("Không thể lưu file tải lên, vui lòng thử lại");
        }
    }

    @Override
    public void deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        try {
            String relativePath = publicUrl.substring(PUBLIC_PREFIX.length());
            Path filePath = rootUploadPath.resolve(relativePath).normalize();
            if (filePath.startsWith(rootUploadPath)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ex) {
            log.warn("Could not delete old file at {}: {}", publicUrl, ex.getMessage());
        }
    }
}
