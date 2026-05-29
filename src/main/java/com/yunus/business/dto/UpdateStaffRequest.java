package com.yunus.business.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Mevcut personel bilgisi güncelleme isteği.
 * profilePhotoUrl ve services bu DTO'da yoktur; ayrı endpoint'lerle yönetilir.
 */
public record UpdateStaffRequest(

        @NotBlank(message = "Personel adı boş olamaz")
        String fullName,

        // Unvan null göndererek temizlenebilir
        String title,

        // Biyografi null göndererek temizlenebilir
        String bio,

        // Sıralama null gelirse mevcut değer korunur (service'te kontrol edilir)
        Integer sortOrder
) {
}
