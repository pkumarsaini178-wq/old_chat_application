package com.example.chatapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepo blacklistedTokenRepo;

    @Transactional
    public void blacklistToken(String token, LocalDateTime expiry) {
        if (token == null || token.isEmpty()) return;
        if (!blacklistedTokenRepo.existsByToken(token)) {
            BlacklistedToken blacklisted = new BlacklistedToken();
            blacklisted.setToken(token);
            blacklisted.setExpiry(expiry);
            blacklistedTokenRepo.save(blacklisted);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) return false;
        return blacklistedTokenRepo.existsByToken(token);
    }

    @Scheduled(fixedRate = 86400000) // Clean up expired tokens once daily
    @Transactional
    public void cleanupExpiredTokens() {
        blacklistedTokenRepo.deleteByExpiryBefore(LocalDateTime.now());
    }
}
