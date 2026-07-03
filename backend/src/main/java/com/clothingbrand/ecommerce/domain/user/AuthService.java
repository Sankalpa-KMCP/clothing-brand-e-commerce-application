package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.exception.DuplicateResourceException;
import com.clothingbrand.ecommerce.exception.InvalidCredentialsException;
import com.clothingbrand.ecommerce.exception.InvalidRefreshTokenException;
import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import com.clothingbrand.ecommerce.config.AccountSecurityProperties;
import com.clothingbrand.ecommerce.security.JwtProperties;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AccountSecurityProperties accountProperties;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       RefreshTokenService refreshTokenService,
                       EmailVerificationService emailVerificationService,
                       PasswordResetService passwordResetService,
                       AccountSecurityProperties accountProperties,
                       com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.accountProperties = accountProperties;
        this.observabilityService = observabilityService;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);
        user.setActive(true);
        user.setLegacyEmailVerificationExempt(false);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException constraintViolationException) {
                if ("users_email_key".equals(constraintViolationException.getConstraintName())) {
                    throw new DuplicateResourceException("Email already exists");
                }
            }
            throw e;
        }

        emailVerificationService.issueVerification(user);

        if (accountProperties.getEmailVerification().isRequired() && !emailVerificationService.isSatisfied(user)) {
            return new VerificationRequiredResponseDto(mapToUserDto(user));
        }

        return getAuthResponse(user);
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            if (!user.getActive()) {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            if (accountProperties.getEmailVerification().isRequired() && !emailVerificationService.isSatisfied(user)) {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            observabilityService.trackLoginSuccess();
            return getAuthResponse(user);
        } catch (InvalidCredentialsException ex) {
            observabilityService.trackLoginFailure();
            throw ex;
        }
    }

    public UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole().getName().name());
        dto.setEmailVerified(emailVerificationService.isSatisfied(user));
        return dto;
    }

    private AuthResponseDto getAuthResponse(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String token = jwtService.generateToken(userDetails);

        RefreshTokenService.RefreshTokenResult result = refreshTokenService.issueToken(user, jwtProperties.getRefreshExpirationMs());

        UserDto userDto = mapToUserDto(user);
        return new AuthResponseDto(token, result.rawToken(), userDto);
    }

    @Transactional
    public TokenRefreshResponseDto refresh(RefreshTokenRequestDto request) {
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.rotateToken(request.getRefreshToken(), jwtProperties.getRefreshExpirationMs())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        User user = userRepository.findById(result.tokenEntity().getUser().getId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String token = jwtService.generateToken(userDetails);

        return new TokenRefreshResponseDto(token, result.rawToken());
    }

    @Transactional
    public void logout(LogoutRequestDto request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
    }

    @Transactional
    public void resendVerification(EmailRequestDto request) {
        emailVerificationService.resend(request.getEmail());
    }

    @Transactional
    public boolean verifyEmail(TokenRequestDto request) {
        return emailVerificationService.verify(request.getToken());
    }

    @Transactional
    public void forgotPassword(EmailRequestDto request) {
        passwordResetService.requestReset(request.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        passwordResetService.resetPassword(request.getToken(), request.getPassword());
    }
}
