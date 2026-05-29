package com.yunus.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Şifre değiştirme isteği.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Mevcut şifre boş bırakılamaz")
        String currentPassword,

        @NotBlank(message = "Yeni şifre boş bırakılamaz")
        @Size(min = 6, max = 32, message = "Şifre en az 6, en fazla 32 karakter olmalıdır")
        String newPassword
) {
}
