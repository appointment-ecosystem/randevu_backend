package com.yunus.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Token yenileme isteği DTO sınıfı.
 */
@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token boş bırakılamaz")
    private String refreshToken;
}
