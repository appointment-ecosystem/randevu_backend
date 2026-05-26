package com.yunus.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Kullanıcı giriş isteği DTO sınıfı.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Telefon numarası boş bırakılamaz")
    private String phone;

    @NotBlank(message = "Şifre boş bırakılamaz")
    private String password;
}
