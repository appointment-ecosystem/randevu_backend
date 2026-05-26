package com.yunus.sms.service;

/**
 * OTP (Tek Kullanımlık Şifre) üretme, gönderme ve doğrulama arayüzü.
 */
public interface OtpService {

    /**
     * Kullanıcı telefon numarası için 6 haneli OTP kodu üretir, Redis'e kaydeder ve SMS ile gönderir.
     *
     * @param phone Alıcı telefon numarası (örn: 5xxxxxxxxx)
     */
    void sendOtp(String phone);

    /**
     * Girilen OTP kodunun geçerliliğini doğrular.
     *
     * @param phone Telefon numarası
     * @param code  Kullanıcının girdiği OTP kodu
     * @return Doğrulama başarılı ise true, aksi takdirde false
     */
    boolean verifyOtp(String phone, String code);

    /**
     * Doğrulanan veya süresi dolan OTP kodunu Redis üzerinden siler.
     *
     * @param phone Telefon numarası
     */
    void invalidateOtp(String phone);
}
