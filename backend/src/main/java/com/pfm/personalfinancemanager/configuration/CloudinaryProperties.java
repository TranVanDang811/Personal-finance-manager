package com.pfm.personalfinancemanager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage.cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private String baseFolder = "pfm";
    private String uploadBaseUrl = "https://api.cloudinary.com/v1_1";
}

