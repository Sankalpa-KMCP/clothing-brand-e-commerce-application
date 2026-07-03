package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.config.AuthRateLimitProperties;
import com.clothingbrand.ecommerce.email.EmailSender;
import com.clothingbrand.ecommerce.email.FakeEmailSender;
import com.clothingbrand.ecommerce.email.TransactionalEmail;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import com.clothingbrand.ecommerce.security.SecureTokenService;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.account.email-verification.required=true",
        "app.account.email-verification.legacy-users-exempt=true",
        "app.email.enabled=true",
        "app.email.mode=fake",
        "app.auth-rate-limit.hash-secret=test-rate-limit-secret"
})
@Transactional
class AccountLifecycleIntegrationTest {

    private static final Pattern TOKEN_QUERY_PATTERN = Pattern.compile("[?&]token=([^&\\s]+)");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecureTokenService secureTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private AuthRateLimitProperties rateLimitProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeEmailSender fakeEmailSender;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        assertInstanceOf(FakeEmailSender.class, emailSender);
        fakeEmailSender = (FakeEmailSender) emailSender;
        fakeEmailSender.clear();
        dateTimeProvider.reset();
        rateLimitProperties.getRules().put("login", new AuthRateLimitProperties.Rule(30, 900));
    }

    @Test
    void registrationWhenVerificationRequiredIssuesNoSessionAndStoresOnlyHashedToken() throws Exception {
        RegisterRequestDto request = registerRequest("phase3-new@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationRequired").value(true))
                .andExpect(jsonPath("$.token").value(nullValue()))
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(jsonPath("$.user.email").value("phase3-new@example.com"))
                .andExpect(jsonPath("$.user.emailVerified").value(false));

        User savedUser = userRepository.findByEmail("phase3-new@example.com").orElseThrow();
        assertNull(savedUser.getEmailVerifiedAt());
        assertFalse(savedUser.getLegacyEmailVerificationExempt());

        String rawToken = latestEmailToken();
        assertEquals(43, rawToken.length());
        String tokenHash = secureTokenService.sha256Hex(rawToken);
        assertTrue(emailVerificationTokenRepository.findByTokenHash(tokenHash).isPresent());
        assertTrue(emailVerificationTokenRepository.findAll().stream()
                .noneMatch(token -> token.getTokenHash().equals(rawToken)));

        login("phase3-new@example.com", "SafePassword123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(rawToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified. You can now sign in."));

        verify(rawToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EmailVerificationService.GENERIC_VERIFY_ERROR));

        login("phase3-new@example.com", "SafePassword123!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    void resendInvalidatesPriorVerificationTokenAndUsesGenericResponses() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("phase3-resend@example.com"))))
                .andExpect(status().isCreated());
        String firstToken = latestEmailToken();

        mockMvc.perform(post("/api/auth/verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"phase3-resend@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(EmailVerificationService.GENERIC_RESEND_MESSAGE));

        String secondToken = latestEmailToken();
        assertNotEquals(firstToken, secondToken);
        EmailVerificationToken firstPersisted = emailVerificationTokenRepository
                .findByTokenHash(secureTokenService.sha256Hex(firstToken))
                .orElseThrow();
        assertNotNull(firstPersisted.getUsedAt());

        verify(firstToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EmailVerificationService.GENERIC_VERIFY_ERROR));
        verify(secondToken)
                .andExpect(status().isOk());

        fakeEmailSender.clear();
        mockMvc.perform(post("/api/auth/verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown-phase3@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(EmailVerificationService.GENERIC_RESEND_MESSAGE));
        assertTrue(fakeEmailSender.getSentEmails().isEmpty());
    }

    @Test
    void expiredVerificationTokenCannotVerifyAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("phase3-expired@example.com"))))
                .andExpect(status().isCreated());
        String rawToken = latestEmailToken();

        EmailVerificationToken persisted = emailVerificationTokenRepository
                .findByTokenHash(secureTokenService.sha256Hex(rawToken))
                .orElseThrow();
        persisted.setExpiresAt(dateTimeProvider.now().minusMinutes(1));
        emailVerificationTokenRepository.saveAndFlush(persisted);

        verify(rawToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EmailVerificationService.GENERIC_VERIFY_ERROR));
    }

    @Test
    void legacyExemptExistingUserCanStillLoginWhenVerificationIsRequired() throws Exception {
        User user = createUser("phase3-legacy@example.com", "SafePassword123!", null, true);

        login(user.getEmail(), "SafePassword123!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    void passwordResetUsesGenericResponsesRevokesRefreshTokensAndInvalidatesAccessTokenVersion() throws Exception {
        User user = createUser("phase3-reset@example.com", "OldPassword123!", dateTimeProvider.now(), false);
        String oldAccessToken = jwtService.generateToken(new UserDetailsImpl(user));

        String loginBody = login(user.getEmail(), "OldPassword123!")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginBody).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown-reset@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_MESSAGE));
        assertTrue(fakeEmailSender.getSentEmails().isEmpty());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"phase3-reset@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_FORGOT_MESSAGE));

        String resetToken = latestEmailToken();
        assertTrue(passwordResetTokenRepository.findByTokenHash(secureTokenService.sha256Hex(resetToken)).isPresent());
        assertTrue(passwordResetTokenRepository.findAll().stream()
                .noneMatch(token -> token.getTokenHash().equals(resetToken)));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPayload(resetToken, "NewPassword123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset. You can now sign in."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPayload(resetToken, "AnotherPassword123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(PasswordResetService.GENERIC_RESET_ERROR));

        login(user.getEmail(), "OldPassword123!")
                .andExpect(status().isUnauthorized());
        login(user.getEmail(), "NewPassword123!")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        RefreshToken oldRefreshToken = refreshTokenRepository
                .findByTokenHash(secureTokenService.sha256Hex(refreshToken))
                .orElseThrow();
        assertEquals(RefreshTokenStatus.REVOKED_PASSWORD_RESET, oldRefreshToken.getStatus());
    }

    @Test
    void rateLimitReturnsJsonRetryAfterAndDoesNotPersistRawEmail() throws Exception {
        rateLimitProperties.getRules().put("login", new AuthRateLimitProperties.Rule(1, 60));
        String email = "phase3-limit@example.com";

        login(email, "WrongPassword123!")
                .andExpect(status().isUnauthorized());

        login(email, "WrongPassword123!")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many attempts. Please try again later."));

        Integer rawEmailCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_rate_limit_buckets WHERE key_hash LIKE ? OR action LIKE ?",
                Integer.class,
                "%" + email + "%",
                "%" + email + "%"
        );
        assertEquals(0, rawEmailCount);
    }

    private RegisterRequestDto registerRequest(String email) {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("Phase");
        request.setLastName("Three");
        request.setEmail(email);
        request.setPassword("SafePassword123!");
        return request;
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginPayload(email, password))));
    }

    private org.springframework.test.web.servlet.ResultActions verify(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verification/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TokenPayload(token))));
    }

    private String latestEmailToken() {
        assertFalse(fakeEmailSender.getSentEmails().isEmpty());
        TransactionalEmail email = fakeEmailSender.getSentEmails().get(fakeEmailSender.getSentEmails().size() - 1);
        Matcher matcher = TOKEN_QUERY_PATTERN.matcher(email.textBody());
        assertTrue(matcher.find(), "Expected token query parameter in fake email body");
        return URI.create("https://example.test/?token=" + matcher.group(1)).getQuery().substring("token=".length());
    }

    private User createUser(String email, String password, OffsetDateTime verifiedAt, boolean legacyExempt) {
        Role role = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Phase");
        user.setLastName("Three");
        user.setRole(role);
        user.setActive(true);
        user.setEmailVerifiedAt(verifiedAt);
        user.setLegacyEmailVerificationExempt(legacyExempt);
        return userRepository.saveAndFlush(user);
    }

    private record LoginPayload(String email, String password) {
    }

    private record TokenPayload(String token) {
    }

    private record ResetPayload(String token, String password) {
    }
}
