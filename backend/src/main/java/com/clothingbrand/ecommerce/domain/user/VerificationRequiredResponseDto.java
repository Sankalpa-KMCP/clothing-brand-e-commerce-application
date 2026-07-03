package com.clothingbrand.ecommerce.domain.user;

public class VerificationRequiredResponseDto extends AuthResponseDto {
    private boolean verificationRequired;

    public VerificationRequiredResponseDto(UserDto user) {
        super(null, null, user);
        this.verificationRequired = true;
    }

    public boolean isVerificationRequired() {
        return verificationRequired;
    }

    public void setVerificationRequired(boolean verificationRequired) {
        this.verificationRequired = verificationRequired;
    }
}
