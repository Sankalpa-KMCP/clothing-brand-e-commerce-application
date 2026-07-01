package com.clothingbrand.ecommerce.domain.user;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequestDto {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
