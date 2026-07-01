package com.clothingbrand.ecommerce.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.status = :newStatus WHERE r.familyId = :familyId")
    int updateStatusByFamilyId(@Param("familyId") UUID familyId, @Param("newStatus") RefreshTokenStatus newStatus);
}
