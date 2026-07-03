package com.clothingbrand.ecommerce.domain.user;

import jakarta.validation.constraints.NotBlank;

public class TokenRequestDto {
    @NotBlank(message = "Token is required")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
