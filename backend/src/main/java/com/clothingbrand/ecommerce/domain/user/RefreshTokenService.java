package com.clothingbrand.ecommerce.domain.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public record RefreshTokenResult(String rawToken, RefreshToken tokenEntity) {}

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public RefreshTokenResult issueToken(User user, long expirationMs) {
        String rawToken = generateRawToken();
        String hash = hashToken(rawToken);

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hash);
        rt.setFamilyId(UUID.randomUUID());
        rt.setUser(user);
        rt.setStatus(RefreshTokenStatus.ACTIVE);
        rt.setExpiresAt(OffsetDateTime.now().plusNanos(expirationMs * 1_000_000L));

        rt = refreshTokenRepository.save(rt);
        return new RefreshTokenResult(rawToken, rt);
    }

    @Transactional
    public Optional<RefreshTokenResult> rotateToken(String rawToken, long expirationMs) {
        String hash = hashToken(rawToken);
        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByTokenHash(hash);

        if (optionalToken.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = optionalToken.get();

        // Must not create successor if expired, inactive user, etc.
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Optional.empty();
        }

        if (token.getUser() == null || !token.getUser().getActive()) {
            return Optional.empty();
        }

        int updated = refreshTokenRepository.updateStatusConditionally(token.getId(), RefreshTokenStatus.ACTIVE, RefreshTokenStatus.REVOKED_ROTATED);

        if (updated == 0) {
            // Failed to consume. Reload to see if it was due to concurrent rotation or past rotation (replay)
            RefreshToken freshToken = refreshTokenRepository.findById(token.getId()).orElseThrow();
            if (freshToken.getStatus() == RefreshTokenStatus.REVOKED_ROTATED) {
                // Replay of a rotated token! Compromise family!
                refreshTokenRepository.updateStatusByFamilyId(token.getFamilyId(), RefreshTokenStatus.REVOKED_COMPROMISED);
            }
            return Optional.empty();
        }

        String newRaw = generateRawToken();
        String newHash = hashToken(newRaw);

        RefreshToken child = new RefreshToken();
        child.setTokenHash(newHash);
        child.setFamilyId(token.getFamilyId());
        child.setUser(token.getUser());
        child.setStatus(RefreshTokenStatus.ACTIVE);
        child.setExpiresAt(OffsetDateTime.now().plusNanos(expirationMs * 1_000_000L));

        child = refreshTokenRepository.save(child);
        return Optional.of(new RefreshTokenResult(newRaw, child));
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            refreshTokenRepository.updateStatusConditionally(token.getId(), RefreshTokenStatus.ACTIVE, RefreshTokenStatus.REVOKED_LOGOUT);
        });
    }
}
