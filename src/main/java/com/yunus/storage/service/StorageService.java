package com.yunus.storage.service;

/**
 * Dosya yükleme ve silme işlemleri için genel arayüz.
 */
public interface StorageService {

    /**
     * Dosyayı byte dizisi olarak yükler ve erişilebilir public URL değerini döner.
     *
     * @param bytes      Yüklenecek dosya içeriği
     * @param fileName   Dosya adı (örn: profile.jpg)
     * @param folderName Yüklenecek klasör (örn: profile-photos)
     * @return Dosyanın genel erişim URL'i
     */
    String uploadFile(byte[] bytes, String fileName, String folderName);

    /**
     * Belirtilen dosyayı depolama alanından siler.
     *
     * @param fileUrl Silinecek dosyanın genel erişim URL'i
     */
    void deleteFile(String fileUrl);
}
