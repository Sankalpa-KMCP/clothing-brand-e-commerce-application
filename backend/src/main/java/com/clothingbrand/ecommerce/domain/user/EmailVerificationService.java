package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.config.AccountSecurityProperties;
import com.clothingbrand.ecommerce.email.TransactionalEmailService;
import com.clothingbrand.ecommerce.security.SecureTokenService;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    public static final String GENERIC_RESEND_MESSAGE = "If the account can receive verification email, a message will be sent shortly.";
    public static final String GENERIC_VERIFY_ERROR = "Verification link is invalid or expired.";

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final AccountSecurityProperties accountProperties;
    private final SecureTokenService secureTokenService;
    private final TransactionalEmailService emailService;
    private final DateTimeProvider dateTimeProvider;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    AccountSecurityProperties accountProperties,
                                    SecureTokenService secureTokenService,
                                    TransactionalEmailService emailService,
                                    DateTimeProvider dateTimeProvider,
                                    com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.accountProperties = accountProperties;
        this.secureTokenService = secureTokenService;
        this.emailService = emailService;
        this.dateTimeProvider = dateTimeProvider;
        this.observabilityService = observabilityService;
    }

    public boolean isSatisfied(User user) {
        if (user.getEmailVerifiedAt() != null) {
            return true;
        }
        return accountProperties.getEmailVerification().isLegacyUsersExempt()
                && Boolean.TRUE.equals(user.getLegacyEmailVerificationExempt());
    }

    @Transactional
    public void issueVerification(User user) {
        if (!accountProperties.getEmailVerification().isEnabled() || user.getEmailVerifiedAt() != null) {
            return;
        }
        tokenRepository.consumeActiveTokensForUser(user.getId(), dateTimeProvider.now());
        String rawToken = secureTokenService.generateOpaqueToken();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(secureTokenService.sha256Hex(rawToken));
        token.setSentToEmail(user.getEmail());
        token.setExpiresAt(dateTimeProvider.now().plusMinutes(accountProperties.getEmailVerification().getTokenExpirationMinutes()));
        tokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), rawToken);
        observabilityService.trackVerificationRequest();
    }

    @Transactional
    public void resend(String email) {
        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmail(normalizedEmail)
                .filter(user -> user.getActive() && user.getEmailVerifiedAt() == null)
                .ifPresent(this::issueVerification);
    }

    @Transactional
    public boolean verify(String rawToken) {
        String tokenHash = secureTokenService.sha256Hex(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (token == null || token.getUsedAt() != null || !token.getExpiresAt().isAfter(dateTimeProvider.now())) {
            observabilityService.trackVerificationFailure();
            return false;
        }

        User user = token.getUser();
        if (user == null || !user.getActive()) {
            observabilityService.trackVerificationFailure();
            return false;
        }

        token.setUsedAt(dateTimeProvider.now());
        user.setEmailVerifiedAt(dateTimeProvider.now());
        user.setLegacyEmailVerificationExempt(false);
        user.incrementAuthVersion();
        tokenRepository.save(token);
        userRepository.save(user);
        tokenRepository.consumeActiveTokensForUser(user.getId(), dateTimeProvider.now());
        observabilityService.trackVerificationSuccess();
        return true;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
