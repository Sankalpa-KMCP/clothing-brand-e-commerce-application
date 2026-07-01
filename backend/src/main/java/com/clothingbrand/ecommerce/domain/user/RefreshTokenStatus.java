package com.clothingbrand.ecommerce.domain.user;

public enum RefreshTokenStatus {
    ACTIVE,
    REVOKED_ROTATED,
    REVOKED_LOGOUT,
    REVOKED_COMPROMISED
}
