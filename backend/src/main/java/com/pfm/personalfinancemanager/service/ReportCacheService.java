package com.pfm.personalfinancemanager.service;

public interface ReportCacheService {

    String REPORT_VERSIONS_CACHE = "report_versions";
    String DASHBOARD_SUMMARY_CACHE = "dashboard_summary";
    String REPORT_ANALYTICS_CACHE = "report_analytics";

    String getUserCacheVersion(String userId);

    void evictUserReports(String userId);
}

