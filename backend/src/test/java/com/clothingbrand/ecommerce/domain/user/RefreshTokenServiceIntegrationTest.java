package com.clothingbrand.ecommerce.domain.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RefreshTokenServiceIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User fixtureUser;
    private final List<UUID> createdFamilyIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdFamilyIds.clear();
        createdUserIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        fixtureUser = new User();
        fixtureUser.setEmail("rt-service-" + UUID.randomUUID() + "@test.com");
        fixtureUser.setFirstName("RT");
        fixtureUser.setLastName("User");
        fixtureUser.setPasswordHash("hash");
        fixtureUser.setRole(customerRole);
        fixtureUser.setActive(true);
        fixtureUser = userRepository.save(fixtureUser);
        createdUserIds.add(fixtureUser.getId());
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (UUID familyId : createdFamilyIds) {
                refreshTokenRepository.findByFamilyId(familyId)
                        .forEach(rt -> refreshTokenRepository.delete(rt));
            }
            refreshTokenRepository.flush();

            for (Long userId : createdUserIds) {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            }
            userRepository.flush();
        });

        transactionTemplate.executeWithoutResult(status -> {
            for (UUID familyId : createdFamilyIds) {
                assertTrue(refreshTokenRepository.findByFamilyId(familyId).isEmpty(), "Leak of family: " + familyId);
            }
            for (Long userId : createdUserIds) {
                assertTrue(userRepository.findById(userId).isEmpty(), "Leak of user: " + userId);
            }
        });
    }

    @Test
    void issuanceCreatesActiveRowWithHashAndNoRawToken() {
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(result.tokenEntity().getFamilyId());

        assertNotNull(result.rawToken());
        assertTrue(result.rawToken().matches("^[A-Za-z0-9_-]{43}$")); // 32 bytes Base64URL without padding is 43 chars

        RefreshToken persisted = refreshTokenRepository.findById(result.tokenEntity().getId()).orElseThrow();
        assertEquals(RefreshTokenStatus.ACTIVE, persisted.getStatus());
        assertEquals(64, persisted.getTokenHash().length());
        assertTrue(persisted.getTokenHash().matches("^[a-f0-9]{64}$")); // SHA-256 hex
        assertNotEquals(result.rawToken(), persisted.getTokenHash());
        assertFalse(persisted.getTokenHash().contains(result.rawToken()));

        // Assert raw token is nowhere in the DB row
        assertEquals(fixtureUser.getId(), persisted.getUser().getId());
    }

    @Test
    void successfulRotationMarksParentRotatedCreatesChildAndPreservesIds() {
        RefreshTokenService.RefreshTokenResult parent = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(parent.tokenEntity().getFamilyId());

        Optional<RefreshTokenService.RefreshTokenResult> childOpt = refreshTokenService.rotateToken(parent.rawToken(), 1000 * 60 * 60);
        assertTrue(childOpt.isPresent());
        RefreshTokenService.RefreshTokenResult child = childOpt.get();

        RefreshToken persistedParent = refreshTokenRepository.findById(parent.tokenEntity().getId()).orElseThrow();
        assertEquals(RefreshTokenStatus.REVOKED_ROTATED, persistedParent.getStatus());

        RefreshToken persistedChild = refreshTokenRepository.findById(child.tokenEntity().getId()).orElseThrow();
        assertEquals(RefreshTokenStatus.ACTIVE, persistedChild.getStatus());

        assertEquals(persistedParent.getFamilyId(), persistedChild.getFamilyId());
        assertEquals(persistedParent.getUser().getId(), persistedChild.getUser().getId());
        assertNotEquals(persistedParent.getTokenHash(), persistedChild.getTokenHash());
    }

    @Test
    void replayOfRotationConsumedTokenCompromisesFamily() {
        RefreshTokenService.RefreshTokenResult parent = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(parent.tokenEntity().getFamilyId());

        Optional<RefreshTokenService.RefreshTokenResult> childOpt = refreshTokenService.rotateToken(parent.rawToken(), 1000 * 60 * 60);
        assertTrue(childOpt.isPresent());

        // Replay the parent
        Optional<RefreshTokenService.RefreshTokenResult> replayOpt = refreshTokenService.rotateToken(parent.rawToken(), 1000 * 60 * 60);
        assertFalse(replayOpt.isPresent());

        // Assert all compromised
        RefreshToken persistedParent = refreshTokenRepository.findById(parent.tokenEntity().getId()).orElseThrow();
        RefreshToken persistedChild = refreshTokenRepository.findById(childOpt.get().tokenEntity().getId()).orElseThrow();

        assertEquals(RefreshTokenStatus.REVOKED_COMPROMISED, persistedParent.getStatus());
        assertEquals(RefreshTokenStatus.REVOKED_COMPROMISED, persistedChild.getStatus());
    }

    @Test
    void compromisedFamilyDoesNotAffectSeparatelyIssuedFamily() {
        RefreshTokenService.RefreshTokenResult fam1 = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(fam1.tokenEntity().getFamilyId());

        RefreshTokenService.RefreshTokenResult fam2 = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(fam2.tokenEntity().getFamilyId());

        refreshTokenService.rotateToken(fam1.rawToken(), 1000 * 60 * 60);
        refreshTokenService.rotateToken(fam1.rawToken(), 1000 * 60 * 60); // Replay -> Compromise Fam1

        RefreshToken persistedFam2 = refreshTokenRepository.findById(fam2.tokenEntity().getId()).orElseThrow();
        assertEquals(RefreshTokenStatus.ACTIVE, persistedFam2.getStatus());

        // Ensure fam2 can still rotate
        Optional<RefreshTokenService.RefreshTokenResult> child2Opt = refreshTokenService.rotateToken(fam2.rawToken(), 1000 * 60 * 60);
        assertTrue(child2Opt.isPresent());
    }

    @Test
    void logoutRevokesOnlySubmittedTokenWithoutCompromise() {
        RefreshTokenService.RefreshTokenResult fam1 = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(fam1.tokenEntity().getFamilyId());

        refreshTokenService.revokeToken(fam1.rawToken());

        RefreshToken persistedFam1 = refreshTokenRepository.findById(fam1.tokenEntity().getId()).orElseThrow();
        assertEquals(RefreshTokenStatus.REVOKED_LOGOUT, persistedFam1.getStatus());

        // Must not create successor
        Optional<RefreshTokenService.RefreshTokenResult> childOpt = refreshTokenService.rotateToken(fam1.rawToken(), 1000 * 60 * 60);
        assertFalse(childOpt.isPresent());

        RefreshToken persistedFam1AfterReplay = refreshTokenRepository.findById(fam1.tokenEntity().getId()).orElseThrow();
        assertEquals(RefreshTokenStatus.REVOKED_LOGOUT, persistedFam1AfterReplay.getStatus()); // Did not transition to compromised
    }

    @Test
    void validationFailuresDoNotCreateSuccessor() {
        RefreshTokenService.RefreshTokenResult token = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(token.tokenEntity().getFamilyId());

        // Expired
        transactionTemplate.executeWithoutResult(status -> {
            RefreshToken t = refreshTokenRepository.findById(token.tokenEntity().getId()).orElseThrow();
            t.setExpiresAt(OffsetDateTime.now().minusDays(1));
            refreshTokenRepository.saveAndFlush(t);
        });

        assertFalse(refreshTokenService.rotateToken(token.rawToken(), 1000 * 60 * 60).isPresent());

        // Unknown
        assertFalse(refreshTokenService.rotateToken("unknown_token_raw_value_12345678901234567890123", 1000 * 60 * 60).isPresent());

        // Missing user / inactive
        RefreshTokenService.RefreshTokenResult token2 = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(token2.tokenEntity().getFamilyId());

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(fixtureUser.getId()).orElseThrow();
            u.setActive(false);
            userRepository.saveAndFlush(u);
        });

        assertFalse(refreshTokenService.rotateToken(token2.rawToken(), 1000 * 60 * 60).isPresent());

        // Restore active for cleanup
        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(fixtureUser.getId()).orElseThrow();
            u.setActive(true);
            userRepository.saveAndFlush(u);
        });
    }

    @Test
    void twoSimultaneousRotationAttemptsFollowStrictCompromiseBehavior() throws InterruptedException {
        RefreshTokenService.RefreshTokenResult initial = refreshTokenService.issueToken(fixtureUser, 1000 * 60 * 60);
        createdFamilyIds.add(initial.tokenEntity().getFamilyId());

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger emptyCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    Optional<RefreshTokenService.RefreshTokenResult> res = refreshTokenService.rotateToken(initial.rawToken(), 1000 * 60 * 60);
                    if (res.isPresent()) {
                        successCount.incrementAndGet();
                    } else {
                        emptyCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        latch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 1 might succeed, 1 must fail and compromise family,
        // Or both fail. However due to atomicity, exact result is 1 succeeds, 1 fails -> then the one that fails compromises the family.
        // Or if both fail it's weird, but Spring transactional handles it.
        // Actually, if 1 succeeds, it returns a new token. But the second one fails and compromises the family.
        // So the new token created by the first one will be marked COMPROMISED.
        List<RefreshToken> familyTokens = refreshTokenRepository.findByFamilyId(initial.tokenEntity().getFamilyId());

        assertEquals(numThreads, successCount.get() + emptyCount.get()); // All threads finished without exception

        // Check family compromise
        boolean allCompromised = familyTokens.stream().allMatch(rt -> rt.getStatus() == RefreshTokenStatus.REVOKED_COMPROMISED);
        assertTrue(allCompromised, "All tokens in family should be REVOKED_COMPROMISED due to concurrent replay");
    }
}
