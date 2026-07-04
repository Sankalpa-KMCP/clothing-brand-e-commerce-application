package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.security.SecureTokenService;
import com.clothingbrand.ecommerce.config.AccountSecurityProperties;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureTokenService secureTokenService;
    private final DateTimeProvider dateTimeProvider;
    private final AccountSecurityProperties accountProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               SecureTokenService secureTokenService,
                               DateTimeProvider dateTimeProvider,
                               AccountSecurityProperties accountProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.secureTokenService = secureTokenService;
        this.dateTimeProvider = dateTimeProvider;
        this.accountProperties = accountProperties;
    }

    public record RefreshTokenResult(String rawToken, RefreshToken tokenEntity) {}

    @Transactional
    public RefreshTokenResult issueToken(User user, long expirationMs) {
        String rawToken = secureTokenService.generateOpaqueToken();
        String hash = secureTokenService.sha256Hex(rawToken);

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hash);
        rt.setFamilyId(UUID.randomUUID());
        rt.setUser(user);
        rt.setStatus(RefreshTokenStatus.ACTIVE);
        rt.setExpiresAt(dateTimeProvider.now().plusNanos(expirationMs * 1_000_000L));

        rt = refreshTokenRepository.save(rt);
        return new RefreshTokenResult(rawToken, rt);
    }

    @Transactional
    public Optional<RefreshTokenResult> rotateToken(String rawToken, long expirationMs) {
        String hash = secureTokenService.sha256Hex(rawToken);
        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByTokenHash(hash);

        if (optionalToken.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = optionalToken.get();

        // Must not create successor if expired, inactive user, etc.
        if (token.getExpiresAt().isBefore(dateTimeProvider.now())) {
            return Optional.empty();
        }

        if (token.getUser() == null || !token.getUser().getActive() || !isEmailVerificationSatisfied(token.getUser())) {
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

        String newRaw = secureTokenService.generateOpaqueToken();
        String newHash = secureTokenService.sha256Hex(newRaw);

        RefreshToken child = new RefreshToken();
        child.setTokenHash(newHash);
        child.setFamilyId(token.getFamilyId());
        child.setUser(token.getUser());
        child.setStatus(RefreshTokenStatus.ACTIVE);
        child.setExpiresAt(dateTimeProvider.now().plusNanos(expirationMs * 1_000_000L));

        child = refreshTokenRepository.save(child);
        return Optional.of(new RefreshTokenResult(newRaw, child));
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String hash = secureTokenService.sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            refreshTokenRepository.updateStatusConditionally(token.getId(), RefreshTokenStatus.ACTIVE, RefreshTokenStatus.REVOKED_LOGOUT);
        });
    }

    @Transactional
    public int revokeActiveTokensForUser(Long userId, RefreshTokenStatus status) {
        return refreshTokenRepository.updateStatusByUserId(userId, RefreshTokenStatus.ACTIVE, status);
    }

    private boolean isEmailVerificationSatisfied(User user) {
        if (!accountProperties.getEmailVerification().isRequired()) {
            return true;
        }
        if (user.getEmailVerifiedAt() != null) {
            return true;
        }
        return accountProperties.getEmailVerification().isLegacyUsersExempt()
                && Boolean.TRUE.equals(user.getLegacyEmailVerificationExempt());
    }
}
