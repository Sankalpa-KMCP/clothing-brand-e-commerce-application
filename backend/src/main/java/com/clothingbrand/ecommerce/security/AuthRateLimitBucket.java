package com.clothingbrand.ecommerce.security;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "auth_rate_limit_buckets")
public class AuthRateLimitBucket {

    @Id
    @Column(name = "key_hash", length = 64)
    private String keyHash;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "window_start", nullable = false)
    private OffsetDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private OffsetDateTime windowEnd;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public OffsetDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(OffsetDateTime windowStart) { this.windowStart = windowStart; }
    public OffsetDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(OffsetDateTime windowEnd) { this.windowEnd = windowEnd; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
