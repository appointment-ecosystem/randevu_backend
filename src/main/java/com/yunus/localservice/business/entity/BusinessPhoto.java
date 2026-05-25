package com.yunus.localservice.business.entity;

import com.yunus.localservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * İşletme galeri fotoğrafının meta verisi.
 * Dosya binary DB'de değil; Cloudflare R2 URL'si burada tutulur.
 */
@Entity
@Table(name = "business_photos")
@Getter
@Setter
@NoArgsConstructor
public class BusinessPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // İşletme listesinde gösterilen kapak görseli; iş kuralı service katmanında tek cover olacak şekilde
    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

}
