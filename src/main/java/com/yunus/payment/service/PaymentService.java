package com.yunus.payment.service;

import com.yunus.payment.dto.request.InitiatePaymentRequest;
import com.yunus.payment.dto.response.InitiatePaymentResponse;
import com.yunus.payment.dto.response.PaymentResponse;
import com.yunus.user.entity.User;
import java.util.Map;
import java.util.UUID;

/**
 * Ödeme işlemlerinin servis sözleşmesi.
 */
public interface PaymentService {

    /**
     * Bir randevu için iyzico 3DS ödeme sürecini başlatır.
     * Kapora tutarı hesaplanır, INITIATED durumunda bir ödeme kaydı oluşturulur
     * ve iyzico'dan dönen 3DS HTML içeriği döndürülür.
     *
     * @param request     kart ve randevu bilgilerini içeren istek
     * @param currentUser ödemeyi başlatan kullanıcı
     * @param clientIp    isteği yapan istemcinin IP adresi
     * @return oluşturulan ödeme kaydının ID'si ve 3DS HTML içeriği
     */
    InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, User currentUser, String clientIp);

    /**
     * iyzico'dan dönen 3DS callback'ini işler.
     * 3DS doğrulamasının sonucuna göre ödeme ve randevu durumunu günceller.
     *
     * @param params iyzico'nun callback isteğiyle gönderdiği form parametreleri
     * @return kullanıcının yönlendirileceği frontend sonuç sayfası adresi
     */
    String handleCallback(Map<String, String> params);

    /**
     * Belirtilen ödeme kaydının güncel durumunu döner.
     *
     * @param paymentId   sorgulanacak ödemenin UUID'si
     * @param currentUser sorguyu yapan kullanıcı
     * @return ödeme kaydının detayları
     */
    PaymentResponse getPaymentStatus(UUID paymentId, User currentUser);

    /**
     * Belirtilen randevuya ait ödeme kaydını döner.
     *
     * @param appointmentId sorgulanacak randevunun UUID'si
     * @param currentUser   sorguyu yapan kullanıcı
     * @return randevuya ait ödeme kaydının detayları
     */
    PaymentResponse getPaymentByAppointment(UUID appointmentId, User currentUser);

    /**
     * Başarılı bir ödemeyi iyzico üzerinden iade eder.
     *
     * @param paymentId   iade edilecek ödemenin UUID'si
     * @param currentUser işlemi yapan kullanıcı
     * @return güncellenmiş ödeme kaydının detayları
     */
    PaymentResponse cancelPayment(UUID paymentId, User currentUser);

}
