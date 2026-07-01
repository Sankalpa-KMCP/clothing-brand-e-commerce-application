package com.clothingbrand.ecommerce.domain.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RefreshTokenIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User fixtureUser;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setEmail("test-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(customerRole);
        user.setActive(true);

        fixtureUser = userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        if (fixtureUser != null && fixtureUser.getId() != null) {
            userRepository.deleteById(fixtureUser.getId());
            userRepository.flush(); // To ensure constraints are checked immediately
            
            // Independent post-cleanup absence verification ensuring FK cascade worked
            assertTrue(refreshTokenRepository.findAll().stream()
                    .noneMatch(rt -> rt.getUser().getId().equals(fixtureUser.getId())));
        }
    }

    @Test
    void testSaveAndRetrieveRefreshToken() {
        String tokenHash = "dummyhash-" + UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(tokenHash);
        rt.setFamilyId(familyId);
        rt.setUser(fixtureUser);
        rt.setStatus(RefreshTokenStatus.ACTIVE);
        rt.setExpiresAt(OffsetDateTime.now().plusDays(7));

        RefreshToken saved = refreshTokenRepository.saveAndFlush(rt);
        assertNotNull(saved.getId());

        Optional<RefreshToken> retrieved = refreshTokenRepository.findByTokenHash(tokenHash);
        assertTrue(retrieved.isPresent());
        assertEquals(familyId, retrieved.get().getFamilyId());
        assertEquals(RefreshTokenStatus.ACTIVE, retrieved.get().getStatus());
    }

    @Test
    void testTokenHashUniquenessEnforced() {
        String tokenHash = "duplicate-hash-" + UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        RefreshToken rt1 = new RefreshToken();
        rt1.setTokenHash(tokenHash);
        rt1.setFamilyId(familyId);
        rt1.setUser(fixtureUser);
        rt1.setStatus(RefreshTokenStatus.ACTIVE);
        rt1.setExpiresAt(OffsetDateTime.now().plusDays(7));
        refreshTokenRepository.saveAndFlush(rt1);

        RefreshToken rt2 = new RefreshToken();
        rt2.setTokenHash(tokenHash);
        rt2.setFamilyId(familyId);
        rt2.setUser(fixtureUser);
        rt2.setStatus(RefreshTokenStatus.ACTIVE);
        rt2.setExpiresAt(OffsetDateTime.now().plusDays(7));
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            refreshTokenRepository.saveAndFlush(rt2);
        });
    }

    @Test
    @Transactional
    void testFamilyLevelRevocation() {
        UUID familyId = UUID.randomUUID();

        RefreshToken rt1 = new RefreshToken();
        rt1.setTokenHash("hash1-" + UUID.randomUUID());
        rt1.setFamilyId(familyId);
        rt1.setUser(fixtureUser);
        rt1.setStatus(RefreshTokenStatus.ACTIVE);
        rt1.setExpiresAt(OffsetDateTime.now().plusDays(7));
        refreshTokenRepository.saveAndFlush(rt1);

        RefreshToken rt2 = new RefreshToken();
        rt2.setTokenHash("hash2-" + UUID.randomUUID());
        rt2.setFamilyId(familyId);
        rt2.setUser(fixtureUser);
        rt2.setStatus(RefreshTokenStatus.REVOKED_ROTATED);
        rt2.setExpiresAt(OffsetDateTime.now().plusDays(7));
        refreshTokenRepository.saveAndFlush(rt2);

        int updated = refreshTokenRepository.updateStatusByFamilyId(familyId, RefreshTokenStatus.REVOKED_COMPROMISED);
        assertEquals(2, updated);

        // Fetch fresh from DB to verify update
        assertEquals(RefreshTokenStatus.REVOKED_COMPROMISED, refreshTokenRepository.findByTokenHash(rt1.getTokenHash()).get().getStatus());
        assertEquals(RefreshTokenStatus.REVOKED_COMPROMISED, refreshTokenRepository.findByTokenHash(rt2.getTokenHash()).get().getStatus());
    }
}
