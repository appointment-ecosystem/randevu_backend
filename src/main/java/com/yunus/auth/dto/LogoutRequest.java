package com.yunus.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Oturum sonlandırma (Logout) isteği DTO sınıfı.
 */
@Getter
@Setter
public class LogoutRequest {

    @NotBlank(message = "Refresh token boş bırakılamaz")
    private String refreshToken;
}
