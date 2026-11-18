package com.shopmind.backend.service;

import com.shopmind.backend.entity.RefreshToken;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private RefreshTokenRepository repo;

    @Value("${jwt.refresh-expiration-days:14}")
    private long ttlDays;

    @Transactional
    public String create(UUID userId, String userAgent) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS));
        token.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255)));
        repo.save(token);
        return raw;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public UUID consume(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        RefreshToken token = repo.findByTokenHash(hash(raw))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (token.isRevoked()) {
            repo.revokeAllByUserId(token.getUserId());
            throw new UnauthorizedException("Refresh token reuse detected, please log in again");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        token.setRevoked(true);
        return token.getUserId();
    }

    @Transactional
    public UUID revoke(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Optional<RefreshToken> opt = repo.findByTokenHash(hash(raw));
        if (opt.isPresent()) {
            RefreshToken t = opt.get();
            t.setRevoked(true);
            return t.getUserId();
        }
        return null;
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repo.revokeAllByUserId(userId);
    }


    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
