package com.pfm.personalfinancemanager.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.pfm.personalfinancemanager.configuration.CloudinaryProperties;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.service.FileStorageService;
import com.pfm.personalfinancemanager.util.ImageFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {

    private static final String UPLOAD_MARKER = "/image/upload/";

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    @Autowired
    public CloudinaryFileStorageService(CloudinaryProperties properties) {
        this(properties, createDefaultRestClient());
    }

    CloudinaryFileStorageService(CloudinaryProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        validateCredentials(properties);
    }

    @Override
    public String storeImage(MultipartFile file, String subDirectory, String ownerPrefix, long maxSizeBytes) {
        String extension = ImageFileValidator.validateAndDetectExtension(file, maxSizeBytes);
        String folder = buildFolderPath(subDirectory);
        String publicId = sanitizeSegment(ownerPrefix) + "_" + UUID.randomUUID();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        TreeMap<String, String> signedParams = new TreeMap<>();
        if (StringUtils.hasText(folder)) {
            signedParams.put("folder", folder);
        }
        signedParams.put("public_id", publicId);
        signedParams.put("timestamp", timestamp);

        String signature = generateSignature(signedParams, properties.getApiSecret());

        try {
            byte[] bytes = file.getBytes();
            String safeFileName = publicId + "." + extension;
            MediaType fileMediaType = MediaType.parseMediaType(ImageFileValidator.resolveMimeType(extension));

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFileName;
                }
            }).contentType(fileMediaType);
            bodyBuilder.part("api_key", properties.getApiKey());
            bodyBuilder.part("timestamp", timestamp);
            if (StringUtils.hasText(folder)) {
                bodyBuilder.part("folder", folder);
            }
            bodyBuilder.part("public_id", publicId);
            bodyBuilder.part("signature", signature);

            MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();
            String endpoint = buildApiUrl("/image/upload");

            JsonNode response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException("Cloudinary không trả về phản hồi hợp lệ");
            }

            String secureUrl = response.path("secure_url").asText(null);
            if (!StringUtils.hasText(secureUrl)) {
                secureUrl = response.path("url").asText(null);
            }
            if (!StringUtils.hasText(secureUrl)) {
                throw new BusinessException("Không nhận được đường dẫn ảnh từ Cloudinary");
            }

            log.debug("Uploaded image to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (IOException ex) {
            log.error("Failed to read uploaded image bytes", ex);
            throw new BusinessException("Không thể đọc dữ liệu ảnh tải lên, vui lòng thử lại");
        } catch (RestClientException ex) {
            log.error("Cloudinary upload failed: {}", ex.getMessage());
            throw new BusinessException("Không thể lưu ảnh lên Cloudinary, vui lòng thử lại sau");
        }
    }

    @Override
    public void deleteByPublicUrl(String publicUrl) {
        String publicId = extractPublicIdFromUrl(publicUrl);
        if (!StringUtils.hasText(publicId)) {
            return;
        }

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        TreeMap<String, String> signedParams = new TreeMap<>();
        signedParams.put("public_id", publicId);
        signedParams.put("timestamp", timestamp);
        String signature = generateSignature(signedParams, properties.getApiSecret());

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("public_id", publicId);
        bodyBuilder.part("api_key", properties.getApiKey());
        bodyBuilder.part("timestamp", timestamp);
        bodyBuilder.part("signature", signature);

        try {
            restClient.post()
                    .uri(buildApiUrl("/image/destroy"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(bodyBuilder.build())
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Deleted Cloudinary image public_id={}", publicId);
        } catch (RestClientException ex) {
            log.warn("Could not delete Cloudinary image {}: {}", publicUrl, ex.getMessage());
        }
    }

    String extractPublicIdFromUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return null;
        }
        String cloudSegment = "/" + properties.getCloudName() + UPLOAD_MARKER;
        int idx = publicUrl.indexOf(cloudSegment);
        if (idx < 0) {
            return null;
        }

        String tail = publicUrl.substring(idx + cloudSegment.length());
        int queryIdx = tail.indexOf('?');
        if (queryIdx >= 0) {
            tail = tail.substring(0, queryIdx);
        }

        // Strip Cloudinary version prefix e.g. v1762850000/
        if (tail.startsWith("v") && tail.contains("/")) {
            int slashIdx = tail.indexOf('/');
            String versionPart = tail.substring(1, slashIdx);
            if (!versionPart.isEmpty() && versionPart.chars().allMatch(Character::isDigit)) {
                tail = tail.substring(slashIdx + 1);
            }
        }

        int dotIdx = tail.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx > tail.lastIndexOf('/')) {
            tail = tail.substring(0, dotIdx);
        }

        String decoded = URLDecoder.decode(tail, StandardCharsets.UTF_8);
        return StringUtils.hasText(decoded) ? decoded : null;
    }

    static String generateSignature(Map<String, String> sortedParams, String apiSecret) {
        StringBuilder payload = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (StringUtils.hasText(entry.getValue())) {
                if (!payload.isEmpty()) {
                    payload.append('&');
                }
                payload.append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        payload.append(apiSecret);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 algorithm unavailable", ex);
        }
    }

    private String buildFolderPath(String subDirectory) {
        String cleanBase = sanitizePath(properties.getBaseFolder());
        String cleanSub = sanitizePath(subDirectory);
        if (StringUtils.hasText(cleanBase) && StringUtils.hasText(cleanSub)) {
            return cleanBase + "/" + cleanSub;
        }
        return StringUtils.hasText(cleanSub) ? cleanSub : cleanBase;
    }

    private String buildApiUrl(String suffix) {
        String baseUrl = properties.getUploadBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + properties.getCloudName() + suffix;
    }

    private static String sanitizePath(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().replaceAll("^/+|/+$", "");
    }

    private static String sanitizeSegment(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "file";
        }
        return raw.trim().replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private static void validateCredentials(CloudinaryProperties props) {
        if (props == null
                || !StringUtils.hasText(props.getCloudName())
                || !StringUtils.hasText(props.getApiKey())
                || !StringUtils.hasText(props.getApiSecret())) {
            throw new IllegalStateException(
                    "Cloudinary storage is enabled but CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, or CLOUDINARY_API_SECRET is missing"
            );
        }
    }

    private static RestClient createDefaultRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return RestClient.builder().requestFactory(factory).build();
    }
}

