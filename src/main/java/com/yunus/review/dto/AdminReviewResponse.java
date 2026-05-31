package com.yunus.review.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin paneli yorum yönetimi için kullanılan okuma (GET) yanıt DTO'su.
 * İşletme adı, kullanıcı adı ve görünürlük bilgisini içerir;
 * moderasyon kararları için gerekli tüm özet bilgiyi tek seferde sunar.
 */
public record AdminReviewResponse(

        /** Yorumun benzersiz kimliği. */
        UUID id,

        /** Yorumun yapıldığı işletmenin kimliği. */
        UUID businessId,

        /** Yorumun yapıldığı işletmenin görünen adı. */
        String businessName,

        /** Yorumu yazan kullanıcının kimliği. */
        UUID userId,

        /** Yorumu yazan kullanıcının tam adı. */
        String userFullName,

        /** Kullanıcının verdiği puan (1–5 arası). */
        Integer rating,

        /** Kullanıcının yazdığı yorum metni; null olabilir. */
        String comment,

        /**
         * Yorumun görünürlük durumu.
         * true = herkese görünür; false = admin tarafından gizlenmiş.
         */
        Boolean isVisible,

        /** Yorumun oluşturulduğu tarih ve saat. */
        OffsetDateTime createdAt
) {
}
