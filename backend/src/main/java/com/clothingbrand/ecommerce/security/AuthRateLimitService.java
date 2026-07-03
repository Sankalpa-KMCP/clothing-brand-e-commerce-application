package com.clothingbrand.ecommerce.security;

import com.clothingbrand.ecommerce.exception.RateLimitExceededException;
import com.clothingbrand.ecommerce.config.AuthRateLimitProperties;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class AuthRateLimitService {

    private final AuthRateLimitBucketRepository bucketRepository;
    private final AuthRateLimitProperties properties;
    private final SecureTokenService secureTokenService;
    private final DateTimeProvider dateTimeProvider;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public AuthRateLimitService(AuthRateLimitBucketRepository bucketRepository,
                                AuthRateLimitProperties properties,
                                SecureTokenService secureTokenService,
                                DateTimeProvider dateTimeProvider,
                                com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.bucketRepository = bucketRepository;
        this.properties = properties;
        this.secureTokenService = secureTokenService;
        this.dateTimeProvider = dateTimeProvider;
        this.observabilityService = observabilityService;
    }

    @Transactional
    public void check(String action, String discriminator) {
        if (!properties.isEnabled()) {
            return;
        }
        AuthRateLimitProperties.Rule rule = properties.getRules().get(action);
        if (rule == null || rule.getLimit() <= 0 || rule.getWindowSeconds() <= 0) {
            return;
        }

        OffsetDateTime now = dateTimeProvider.now();
        String key = secureTokenService.hmacSha256Hex(
                properties.getHashSecret(),
                action + "|" + normalize(discriminator)
        );

        AuthRateLimitBucket bucket = bucketRepository.findByKeyHashForUpdate(key).orElse(null);
        if (bucket == null) {
            bucket = new AuthRateLimitBucket();
            bucket.setKeyHash(key);
            bucket.setAction(action);
            bucket.setWindowStart(now);
            bucket.setWindowEnd(now.plusSeconds(rule.getWindowSeconds()));
            bucket.setAttemptCount(1);
            bucket.setUpdatedAt(now);
            try {
                bucketRepository.saveAndFlush(bucket);
                return;
            } catch (DataIntegrityViolationException ignored) {
                bucket = bucketRepository.findByKeyHashForUpdate(key).orElseThrow();
            }
        }

        if (!bucket.getWindowEnd().isAfter(now)) {
            bucket.setWindowStart(now);
            bucket.setWindowEnd(now.plusSeconds(rule.getWindowSeconds()));
            bucket.setAttemptCount(1);
            bucket.setUpdatedAt(now);
            bucketRepository.save(bucket);
            return;
        }

        if (bucket.getAttemptCount() >= rule.getLimit()) {
            long retryAfter = Math.max(1, Duration.between(now, bucket.getWindowEnd()).getSeconds());
            observabilityService.trackRateLimitRejection();
            throw new RateLimitExceededException("Too many attempts. Please try again later.", retryAfter);
        }

        bucket.setAttemptCount(bucket.getAttemptCount() + 1);
        bucket.setUpdatedAt(now);
        bucketRepository.save(bucket);
    }

    @Transactional
    public int cleanupExpiredBuckets() {
        return bucketRepository.deleteExpiredBefore(dateTimeProvider.now().minusDays(1));
    }

    public boolean isTrustForwardedFor() {
        return properties.isTrustForwardedFor();
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }
}
