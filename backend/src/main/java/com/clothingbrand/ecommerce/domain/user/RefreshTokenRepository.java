package com.clothingbrand.ecommerce.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyId(UUID familyId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.status = :newStatus WHERE r.familyId = :familyId")
    int updateStatusByFamilyId(@Param("familyId") UUID familyId, @Param("newStatus") RefreshTokenStatus newStatus);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.status = :newStatus WHERE r.user.id = :userId AND r.status = :expectedStatus")
    int updateStatusByUserId(@Param("userId") Long userId, @Param("expectedStatus") RefreshTokenStatus expectedStatus, @Param("newStatus") RefreshTokenStatus newStatus);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.status = :newStatus WHERE r.id = :id AND r.status = :expectedStatus")
    int updateStatusConditionally(@Param("id") Long id, @Param("expectedStatus") RefreshTokenStatus expectedStatus, @Param("newStatus") RefreshTokenStatus newStatus);
}
