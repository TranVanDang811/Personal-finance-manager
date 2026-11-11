package com.pfm.personalfinancemanager.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeImage(MultipartFile file, String subDirectory, String ownerPrefix, long maxSizeBytes);

    void deleteByPublicUrl(String publicUrl);
}

