package com.yunus.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Kullanıcı giriş isteği DTO sınıfı.
 */
@Getter
@Setter
public class LoginRequest {

    private String phone;
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String email;

    @NotBlank(message = "Şifre boş bırakılamaz")
    private String password;
}
