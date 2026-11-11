package com.pfm.personalfinancemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {

    private String status;
    private String service;
    private String version;
    private String profile;
    private String databaseStatus;
    private Instant timestamp;
}

