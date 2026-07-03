package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.config.AccountSecurityProperties;
import com.clothingbrand.ecommerce.email.TransactionalEmailService;
import com.clothingbrand.ecommerce.exception.InvalidCredentialsException;
import com.clothingbrand.ecommerce.security.SecureTokenService;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    public static final String GENERIC_FORGOT_MESSAGE = "If the account can receive password reset email, a message will be sent shortly.";
    public static final String GENERIC_RESET_ERROR = "Password reset link is invalid or expired.";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AccountSecurityProperties accountProperties;
    private final SecureTokenService secureTokenService;
    private final TransactionalEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final DateTimeProvider dateTimeProvider;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                RefreshTokenService refreshTokenService,
                                AccountSecurityProperties accountProperties,
                                SecureTokenService secureTokenService,
                                TransactionalEmailService emailService,
                                PasswordEncoder passwordEncoder,
                                DateTimeProvider dateTimeProvider,
                                com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.accountProperties = accountProperties;
        this.secureTokenService = secureTokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.dateTimeProvider = dateTimeProvider;
        this.observabilityService = observabilityService;
    }

    @Transactional
    public void requestReset(String email) {
        if (!accountProperties.getPasswordReset().isEnabled()) {
            return;
        }
        observabilityService.trackResetRequest();
        userRepository.findByEmail(normalizeEmail(email))
                .filter(User::getActive)
                .ifPresent(user -> {
                    tokenRepository.consumeActiveTokensForUser(user.getId(), dateTimeProvider.now());
                    String rawToken = secureTokenService.generateOpaqueToken();

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setTokenHash(secureTokenService.sha256Hex(rawToken));
                    token.setExpiresAt(dateTimeProvider.now().plusMinutes(accountProperties.getPasswordReset().getTokenExpirationMinutes()));
                    tokenRepository.save(token);

                    emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        try {
            String tokenHash = secureTokenService.sha256Hex(rawToken);
            PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new InvalidCredentialsException(GENERIC_RESET_ERROR));

            if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(dateTimeProvider.now())) {
                throw new InvalidCredentialsException(GENERIC_RESET_ERROR);
            }

            User user = token.getUser();
            if (user == null || !user.getActive()) {
                throw new InvalidCredentialsException(GENERIC_RESET_ERROR);
            }

            token.setUsedAt(dateTimeProvider.now());
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.incrementAuthVersion();
            tokenRepository.save(token);
            userRepository.save(user);
            tokenRepository.consumeActiveTokensForUser(user.getId(), dateTimeProvider.now());
            refreshTokenService.revokeActiveTokensForUser(user.getId(), RefreshTokenStatus.REVOKED_PASSWORD_RESET);
            observabilityService.trackResetSuccess();
        } catch (RuntimeException ex) {
            observabilityService.trackResetFailure();
            throw ex;
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
