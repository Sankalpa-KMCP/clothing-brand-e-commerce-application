package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import com.clothingbrand.ecommerce.security.AuthRateLimitService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimitService rateLimitService;

    public AuthController(AuthService authService, AuthRateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request, HttpServletRequest servletRequest) {
        rateLimitService.check("register", clientKey(servletRequest) + "|" + request.getEmail());
        AuthResponseDto response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest servletRequest) {
        rateLimitService.check("login", clientKey(servletRequest) + "|" + request.getEmail());
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request, HttpServletRequest servletRequest) {
        rateLimitService.check("refresh", clientKey(servletRequest));
        TokenRefreshResponseDto response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequestDto request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verification/resend")
    public ResponseEntity<GenericAuthMessageResponseDto> resendVerification(@Valid @RequestBody EmailRequestDto request,
                                                                            HttpServletRequest servletRequest) {
        rateLimitService.check("resend-verification", clientKey(servletRequest) + "|" + request.getEmail());
        authService.resendVerification(request);
        return ResponseEntity.ok(new GenericAuthMessageResponseDto(EmailVerificationService.GENERIC_RESEND_MESSAGE));
    }

    @PostMapping("/verification/verify")
    public ResponseEntity<GenericAuthMessageResponseDto> verifyEmail(@Valid @RequestBody TokenRequestDto request) {
        boolean verified = authService.verifyEmail(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericAuthMessageResponseDto(EmailVerificationService.GENERIC_VERIFY_ERROR));
        }
        return ResponseEntity.ok(new GenericAuthMessageResponseDto("Email verified. You can now sign in."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GenericAuthMessageResponseDto> forgotPassword(@Valid @RequestBody EmailRequestDto request,
                                                                        HttpServletRequest servletRequest) {
        rateLimitService.check("forgot-password", clientKey(servletRequest) + "|" + request.getEmail());
        authService.forgotPassword(request);
        return ResponseEntity.ok(new GenericAuthMessageResponseDto(PasswordResetService.GENERIC_FORGOT_MESSAGE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GenericAuthMessageResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request,
                                                                       HttpServletRequest servletRequest) {
        rateLimitService.check("reset-password", clientKey(servletRequest));
        authService.resetPassword(request);
        return ResponseEntity.ok(new GenericAuthMessageResponseDto("Password reset. You can now sign in."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            // Unlikely to reach here if security config works correctly, but safe to check
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto userDto = authService.mapToUserDto(userDetails.getUser());
        return ResponseEntity.ok(userDto);
    }

    private String clientKey(HttpServletRequest request) {
        if (rateLimitService.isTrustForwardedFor()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
