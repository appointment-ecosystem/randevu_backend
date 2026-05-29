package com.yunus.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Telefon OTP doğrulama isteği.
 */
public record PhoneVerifyRequest(
        @NotBlank(message = "OTP kodu boş bırakılamaz")
        @Pattern(regexp = "^\\d{6}$", message = "OTP kodu 6 haneli olmalıdır")
        String otpCode
) {
}
