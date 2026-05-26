package com.yunus.storage.service;

import com.yunus.common.exception.BusinessException;
import com.yunus.storage.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.util.UUID;

/**
 * Cloudflare R2 / S3 uyumlu storage servisi implementasyonu.
 * Dosyaları belirtilen klasör yapısında, çakışmayı önlemek için benzersiz UUID isimleriyle saklar.
 */
@Service
public class R2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public R2StorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String uploadFile(byte[] bytes, String fileName, String folderName) {
        // Dosya uzantısını al (örn: .png, .jpg)
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
        }

        // Benzersiz bir dosya adı oluştur
        String uniqueFileName = folderName + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.bucketName())
                    .key(uniqueFileName)
                    // Standart tarayıcı önizlemesi için dosya uzantısına göre content-type belirlenebilir,
                    // burada basitlik adına binary stream veya default ayarlanmıştır.
                    .contentType(detectContentType(extension))
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            log.info("File successfully uploaded to storage: {}", uniqueFileName);

            // Public erişim URL'ini dön
            return storageProperties.publicUrl() + "/" + uniqueFileName;
        } catch (Exception ex) {
            log.error("Failed to upload file to storage: {}", fileName, ex);
            throw new BusinessException("Dosya yükleme işlemi başarısız oldu", ex);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        // URL'den key kısmını çıkar (örn: https://cdn.example.com/profile/abc.jpg -> profile/abc.jpg)
        String publicUrl = storageProperties.publicUrl();
        if (!fileUrl.startsWith(publicUrl)) {
            log.warn("Attempt to delete file with foreign URL: {}", fileUrl);
            return;
        }

        String fileKey = fileUrl.substring(publicUrl.length() + 1); // +1 slash karakterini atlar

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(storageProperties.bucketName())
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File successfully deleted from storage: {}", fileKey);
        } catch (Exception ex) {
            log.error("Failed to delete file from storage: {}", fileKey, ex);
            throw new BusinessException("Dosya silme işlemi başarısız oldu", ex);
        }
    }

    private String detectContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
