package com.yunus.auth.dto;

import com.yunus.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Kullanıcı kayıt isteği DTO sınıfı.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Ad soyad boş bırakılamaz")
    @Size(max = 100, message = "Ad soyad en fazla 100 karakter olabilir")
    private String fullName;

    @NotBlank(message = "Telefon numarası boş bırakılamaz")
    @Pattern(regexp = "^(05|5)\\d{9}$", message = "Lütfen geçerli bir Türkiye cep telefonu numarası giriniz (örn: 5xxxxxxxxx veya 05xxxxxxxxx)")
    private String phone;

    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz")
    @Size(max = 150, message = "E-posta en fazla 150 karakter olabilir")
    private String email;

    @NotBlank(message = "Şifre boş bırakılamaz")
    @Size(min = 6, max = 32, message = "Şifre en az 6, en fazla 32 karakter olmalıdır")
    private String password;

    @NotNull(message = "Rol belirtilmelidir")
    private UserRole role;
}
