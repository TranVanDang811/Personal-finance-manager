package com.pfm.personalfinancemanager.security;

import java.time.Duration;

public interface TokenBlacklistService {

    void blacklistToken(String jti, Duration ttl);

    boolean isTokenBlacklisted(String jti);
}
