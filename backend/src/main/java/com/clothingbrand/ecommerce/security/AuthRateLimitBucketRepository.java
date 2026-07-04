package com.clothingbrand.ecommerce.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface AuthRateLimitBucketRepository extends JpaRepository<AuthRateLimitBucket, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AuthRateLimitBucket b WHERE b.keyHash = :keyHash")
    Optional<AuthRateLimitBucket> findByKeyHashForUpdate(@Param("keyHash") String keyHash);

    @Modifying
    @Query("DELETE FROM AuthRateLimitBucket b WHERE b.windowEnd < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") OffsetDateTime cutoff);
}
