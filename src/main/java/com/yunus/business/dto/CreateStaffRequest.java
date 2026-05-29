package com.yunus.business.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Yeni personel oluşturma isteği.
 * profilePhotoUrl ve services bu DTO'da yoktur; ayrı endpoint'lerle yönetilir.
 */
public record CreateStaffRequest(

        @NotBlank(message = "Personel adı boş olamaz")
        String fullName,

        // Unvan isteğe bağlı; null gelebilir
        String title,

        // Kısa biyografi isteğe bağlı; null gelebilir
        String bio,

        // Sıralama belirtilmezse compact constructor 0 atar
        Integer sortOrder
) {
    /**
     * Compact constructor: sortOrder null gelirse varsayılan 0 uygulanır.
     * Record'larda field initializer olmadığı için bu pattern kullanılır.
     */
    public CreateStaffRequest {
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
