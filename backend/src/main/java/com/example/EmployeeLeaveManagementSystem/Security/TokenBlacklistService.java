package com.example.EmployeeLeaveManagementSystem.Security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of logged-out tokens (kept only until they would have expired anyway).
 * For multi-instance deployments replace with a shared store such as Redis.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, Date expiresAt) {
        purgeExpired();
        blacklist.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    private void purgeExpired() {
        Date now = new Date();
        blacklist.values().removeIf(expiry -> expiry.before(now));
    }
}
