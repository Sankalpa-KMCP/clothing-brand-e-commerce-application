package com.clothingbrand.ecommerce.security;

import com.clothingbrand.ecommerce.config.AccountSecurityProperties;
import com.clothingbrand.ecommerce.domain.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final AccountSecurityProperties accountProperties;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   CustomAuthenticationEntryPoint authenticationEntryPoint,
                                   AccountSecurityProperties accountProperties) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accountProperties = accountProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            if (jwtService.validateToken(jwt)) {
                userEmail = jwtService.extractEmail(jwt);

                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                    // UserDetails implementation checks if active (isEnabled())
                    User user = userDetails instanceof UserDetailsImpl userDetailsImpl ? userDetailsImpl.getUser() : null;
                    Long tokenAuthVersion = jwtService.extractAuthVersion(jwt);
                    Long userAuthVersion = user == null ? 0L : user.getAuthVersion();
                    if (userDetails.isEnabled()
                            && tokenAuthVersion.equals(userAuthVersion == null ? 0L : userAuthVersion)
                            && isEmailVerificationSatisfied(user)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        authenticationEntryPoint.commence(request, response, new BadCredentialsException("User account is inactive or disabled"));
                        return;
                    }
                }
            } else {
                authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid or expired token"));
                return;
            }
        } catch (Exception e) {
            authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid or expired token"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isEmailVerificationSatisfied(User user) {
        if (user == null || !accountProperties.getEmailVerification().isRequired()) {
            return true;
        }
        if (user.getEmailVerifiedAt() != null) {
            return true;
        }
        return accountProperties.getEmailVerification().isLegacyUsersExempt()
                && Boolean.TRUE.equals(user.getLegacyEmailVerificationExempt());
    }
}
