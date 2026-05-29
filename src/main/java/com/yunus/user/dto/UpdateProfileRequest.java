package com.yunus.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Profil güncelleme isteği.
 * Alanların ikisi de opsiyoneldir.
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "Ad soyad en fazla 100 karakter olabilir")
        String fullName,

        @Email(message = "Lütfen geçerli bir e-posta adresi giriniz")
        @Size(max = 150, message = "E-posta en fazla 150 karakter olabilir")
        String email
) {
}
