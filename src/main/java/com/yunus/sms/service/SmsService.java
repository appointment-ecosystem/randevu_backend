package com.yunus.sms.service;

/**
 * SMS gönderme işlemlerini başlatan arayüz.
 */
public interface SmsService {

    /**
     * Alıcı telefona SMS mesajı gönderir.
     *
     * @param phone   Alıcı telefon numarası (örn: 5xxxxxxxxx veya 05xxxxxxxxx)
     * @param message Gönderilecek mesaj metni
     */
    void sendSms(String phone, String message);
}
