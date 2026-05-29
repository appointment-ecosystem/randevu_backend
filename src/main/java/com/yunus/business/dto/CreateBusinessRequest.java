package com.yunus.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * İşletme oluşturma isteği.
 */
public record CreateBusinessRequest(
        @NotBlank(message = "İşletme adı boş bırakılamaz")
        @Size(max = 150, message = "İşletme adı en fazla 150 karakter olabilir")
        String name,

        String description,

        @Size(max = 20, message = "Telefon en fazla 20 karakter olabilir")
        String phone,

        @Email(message = "Lütfen geçerli bir e-posta adresi giriniz")
        @Size(max = 150, message = "E-posta en fazla 150 karakter olabilir")
        String email,

        @Size(max = 300, message = "Web sitesi adresi en fazla 300 karakter olabilir")
        String website,

        @NotNull(message = "Şehir seçimi zorunludur")
        UUID cityId,

        @NotNull(message = "İlçe seçimi zorunludur")
        UUID districtId,

        @NotNull(message = "Mahalle seçimi zorunludur")
        UUID neighborhoodId,

        @Size(max = 300, message = "Adres satırı en fazla 300 karakter olabilir")
        String addressLine,

        Double latitude,

        Double longitude,

        @NotNull(message = "En az bir kategori seçilmelidir")
        List<UUID> categoryIds
) {
}

