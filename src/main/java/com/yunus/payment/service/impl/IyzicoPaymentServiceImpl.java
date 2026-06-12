package com.yunus.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.model.Refund;
import com.iyzipay.model.ThreedsInitialize;
import com.iyzipay.model.ThreedsPayment;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.CreateThreedsPaymentRequest;
import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.payment.config.IyzicoProperties;
import com.yunus.payment.dto.request.InitiatePaymentRequest;
import com.yunus.payment.dto.response.InitiatePaymentResponse;
import com.yunus.payment.dto.response.PaymentResponse;
import com.yunus.payment.entity.Payment;
import com.yunus.payment.enums.PaymentStatus;
import com.yunus.payment.enums.PaymentType;
import com.yunus.payment.repository.PaymentRepository;
import com.yunus.payment.service.PaymentService;
import com.yunus.user.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * iyzico 3DS ödeme entegrasyon servisi.
 *
 * <p>Akış özeti:
 * <ol>
 *   <li>{@link #initiatePayment} — randevu için kapora tutarı hesaplanır,
 *       INITIATED durumunda ödeme kaydı oluşturulur ve iyzico'dan 3DS HTML alınır.</li>
 *   <li>{@link #handleCallback} — iyzico'nun 3DS sonrası yönlendirdiği callback işlenir;
 *       ödeme ve randevu durumu güncellenir.</li>
 *   <li>{@link #cancelPayment} — başarılı bir ödeme iyzico üzerinden iade edilir.</li>
 * </ol>
 *
 * <p>Güvenlik: Kart bilgileri (numara, son kullanma tarihi, CVC) hiçbir koşulda
 * loglanmaz veya veritabanına yazılmaz; yalnızca iyzico SDK çağrısında kullanılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoPaymentServiceImpl implements PaymentService {

    // Ödeme başlatılabilecek randevu durumları
    private static final List<AppointmentStatus> PAYABLE_APPOINTMENT_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    // 3DS ve iyzico API'lerinden dönen başarı durumu metni
    private static final String IYZICO_SUCCESS_STATUS = "success";

    // 3DS doğrulamasının başarılı olduğunu gösteren mdStatus değeri
    private static final String MD_STATUS_SUCCESS = "1";

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final Options iyzicoOptions;
    private final IyzicoProperties iyzicoProperties;
    private final ObjectMapper objectMapper;

    // Ödeme sonucu sonrası kullanıcının yönlendirileceği frontend adresi
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // =========================================================================
    // Ödeme başlatma
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Adımlar: randevu doğrulaması → tekrar ödeme kontrolü → kapora tutarı
     * hesaplama → INITIATED ödeme kaydı → iyzico 3DS başlatma → PENDING'e geçiş.
     */
    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, User currentUser, String clientIp) {
        // Adım 1 — Randevuyu bul
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new BusinessException("Randevu bulunamadı: " + request.appointmentId()));

        // Adım 2 — Randevu, giriş yapmış kullanıcıya ait olmalı
        if (!appointment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bu randevu için ödeme başlatma yetkiniz yok.");
        }

        // Adım 3 — Randevu durumu ödemeye uygun olmalı
        if (!PAYABLE_APPOINTMENT_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessException("Bu randevu için ödeme başlatılamaz");
        }

        // Adım 4 — Bu randevu için zaten başarılı bir ödeme var mı?
        paymentRepository.findByAppointmentIdAndStatusIn(appointment.getId(), List.of(PaymentStatus.SUCCESS))
                .ifPresent(existing -> {
                    throw new BusinessException("Bu randevu için zaten ödeme alınmış");
                });

        // Adım 5 — Kapora tutarını hesapla
        BigDecimal depositAmount = appointment.getPriceSnapshot()
                .multiply(BigDecimal.valueOf(iyzicoProperties.getDepositRate()))
                .setScale(2, RoundingMode.HALF_UP);

        // Adım 6 — INITIATED durumunda ödeme kaydı oluştur
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setUser(currentUser);
        payment.setAmount(depositAmount);
        payment.setCurrency("TRY");
        payment.setPaymentType(PaymentType.DEPOSIT);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setProvider("IYZICO");
        payment = paymentRepository.save(payment);

        // Adım 7-8 — iyzico 3DS başlatma isteğini oluştur ve gönder
        CreatePaymentRequest threedsRequest = buildThreedsRequest(payment, appointment, currentUser, request, clientIp);
        ThreedsInitialize threedsInitialize = ThreedsInitialize.create(threedsRequest, iyzicoOptions);

        // TODO: geçici debug logu - sorun çözülünce kaldırılacak
        log.warn("iyzico yanıtı: status={}, errorCode={}, errorMessage={}, errorGroup={}, locale={}",
                threedsInitialize.getStatus(),
                threedsInitialize.getErrorCode(),
                threedsInitialize.getErrorMessage(),
                threedsInitialize.getErrorGroup(),
                threedsInitialize.getLocale());

        // Adım 9 — Başlatma başarısızsa ödemeyi FAIL olarak işaretle
        if (!IYZICO_SUCCESS_STATUS.equals(threedsInitialize.getStatus())) {
            payment.setStatus(PaymentStatus.FAIL);
            paymentRepository.save(payment);
            log.warn("3DS başlatma başarısız: paymentId={}", payment.getId());
            throw new BusinessException("Ödeme başlatılamadı, lütfen tekrar deneyin.");
        }

        // Adım 10 — Ödeme kaydını PENDING'e geçir
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Adım 11 — Loglama (kart bilgisi içermez)
        log.info("Ödeme başlatıldı: paymentId={}, appointmentId={}, tutar={}",
                payment.getId(), appointment.getId(), payment.getAmount());

        // Adım 12 — Yanıtı döndür
        return new InitiatePaymentResponse(payment.getId(), threedsInitialize.getHtmlContent());
    }

    /**
     * iyzico 3DS başlatma isteğini (kart, alıcı, adres ve sepet bilgileriyle) oluşturur.
     * Kart bilgileri yalnızca bu istek nesnesinde tutulur, kalıcı hale getirilmez.
     *
     * @param payment     INITIATED durumundaki ödeme kaydı
     * @param appointment ödemeye konu olan randevu
     * @param user        ödemeyi yapan kullanıcı
     * @param request     kart bilgilerini içeren istek
     * @param clientIp    isteği yapan istemcinin IP adresi
     * @return iyzico SDK'sına gönderilecek ödeme isteği
     */
    private CreatePaymentRequest buildThreedsRequest(Payment payment, Appointment appointment, User user,
                                                       InitiatePaymentRequest request, String clientIp) {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setLocale(Locale.TR.getValue());
        // Callback'te bu değer geri gelir; Payment kaydını bununla buluruz
        paymentRequest.setConversationId(payment.getId().toString());
        paymentRequest.setPrice(payment.getAmount());
        paymentRequest.setPaidPrice(payment.getAmount());
        paymentRequest.setCurrency(Currency.TRY.name());
        paymentRequest.setInstallment(1);
        paymentRequest.setBasketId(appointment.getId().toString());
        paymentRequest.setPaymentChannel("WEB");
        paymentRequest.setPaymentGroup("SERVICE");
        paymentRequest.setCallbackUrl(iyzicoProperties.getCallbackUrl());

        // TODO: geçici debug logu - sorun çözülünce kaldırılacak (kart bilgisi içermez)
        log.info("iyzico request: conversationId={}, price={}, callbackUrl={}, buyerEmail={}, buyerId={}",
                paymentRequest.getConversationId(),
                paymentRequest.getPrice(),
                iyzicoProperties.getCallbackUrl(),
                user.getEmail() != null ? user.getEmail() : "null",
                user.getId() != null ? user.getId().toString() : "null");

        // Kart bilgileri — ASLA loglanmaz veya saklanmaz
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(request.cardHolderName());
        paymentCard.setCardNumber(request.cardNumber().replaceAll("\\s", ""));
        paymentCard.setExpireMonth(request.expireMonth());
        paymentCard.setExpireYear(request.expireYear());
        paymentCard.setCvc(request.cvc());
        paymentCard.setRegisterCard(0);
        paymentRequest.setPaymentCard(paymentCard);

        // Alıcı bilgileri
        Buyer buyer = new Buyer();
        buyer.setId(user.getId().toString());
        buyer.setName(ilkAd(user.getFullName()));
        buyer.setSurname(sonSoyad(user.getFullName()));
        buyer.setIdentityNumber(request.buyerIdentityNumber() != null ? request.buyerIdentityNumber() : "11111111111");
        buyer.setEmail(user.getEmail() != null ? user.getEmail() : user.getPhone() + "@yakinhizmet.com");
        buyer.setGsmNumber(user.getPhone());
        buyer.setRegistrationAddress("Türkiye");
        buyer.setIp(clientIp);
        buyer.setCity("İzmir");
        buyer.setCountry("Turkey");
        paymentRequest.setBuyer(buyer);

        // Teslimat ve fatura adresi aynı (hizmet randevusu için sabit adres)
        Address address = new Address();
        address.setContactName(user.getFullName());
        address.setCity("İzmir");
        address.setCountry("Turkey");
        address.setAddress("Türkiye");
        paymentRequest.setShippingAddress(address);
        paymentRequest.setBillingAddress(address);

        // Sepet — randevuya konu olan tek hizmet kalemi
        BasketItem basketItem = new BasketItem();
        basketItem.setId(appointment.getId().toString());
        basketItem.setName(appointment.getService().getName());
        basketItem.setCategory1("Randevu Hizmeti");
        basketItem.setItemType(BasketItemType.VIRTUAL.name());
        basketItem.setPrice(payment.getAmount());
        paymentRequest.setBasketItems(List.of(basketItem));

        return paymentRequest;
    }

    // =========================================================================
    // 3DS callback işleme
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>conversationId ile ödeme kaydı bulunur, mdStatus kontrol edilir ve
     * iyzico'ya 3DS doğrulama tamamlama isteği gönderilir. Sonuca göre ödeme
     * SUCCESS/FAIL olarak güncellenir; başarılıysa randevu CONFIRMED'a geçirilir.
     */
    @Override
    @Transactional
    public String handleCallback(Map<String, String> params) {
        String conversationId = params.get("conversationId");
        String iyzicoPaymentId = params.get("paymentId");
        String conversationData = params.get("conversationData");
        String mdStatus = params.get("mdStatus");

        log.info("Ödeme callback alındı: conversationId={}, mdStatus={}", conversationId, mdStatus);

        if (conversationId == null) {
            return frontendUrl + "/odeme/sonuc?status=hata";
        }

        Payment payment = findPaymentByConversationId(conversationId);
        if (payment == null) {
            return frontendUrl + "/odeme/sonuc?status=hata";
        }

        try {
            // mdStatus "1" değilse 3DS doğrulaması başarısız demektir
            if (!MD_STATUS_SUCCESS.equals(mdStatus)) {
                payment.setStatus(PaymentStatus.FAIL);
                paymentRepository.save(payment);
                log.warn("3DS doğrulama başarısız: paymentId={}, mdStatus={}", payment.getId(), mdStatus);
                return frontendUrl + "/odeme/sonuc?status=basarisiz&id=" + payment.getId();
            }

            // 3DS doğrulamasını tamamlamak için iyzico'ya istek gönder
            CreateThreedsPaymentRequest threedsPaymentRequest = new CreateThreedsPaymentRequest();
            threedsPaymentRequest.setLocale(Locale.TR.getValue());
            threedsPaymentRequest.setConversationId(conversationId);
            threedsPaymentRequest.setPaymentId(iyzicoPaymentId);
            threedsPaymentRequest.setConversationData(conversationData);

            ThreedsPayment threedsPayment = ThreedsPayment.create(threedsPaymentRequest, iyzicoOptions);

            // Kart bilgisi içermeyen güvenli bir özet kaydet
            saveProviderResponse(payment, threedsPayment);

            if (IYZICO_SUCCESS_STATUS.equals(threedsPayment.getStatus())) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderReference(threedsPayment.getPaymentId());
                if (threedsPayment.getPaymentItems() != null && !threedsPayment.getPaymentItems().isEmpty()) {
                    payment.setProviderTransactionReference(
                            threedsPayment.getPaymentItems().get(0).getPaymentTransactionId());
                }
                paymentRepository.save(payment);

                Appointment appointment = payment.getAppointment();
                appointment.setStatus(AppointmentStatus.CONFIRMED);
                appointmentRepository.save(appointment);

                log.info("Ödeme başarılı: paymentId={}, appointmentId={}", payment.getId(), appointment.getId());
                return frontendUrl + "/odeme/sonuc?status=basarili&id=" + payment.getId();
            } else {
                payment.setStatus(PaymentStatus.FAIL);
                paymentRepository.save(payment);
                log.warn("Ödeme onaylama başarısız: paymentId={}", payment.getId());
                return frontendUrl + "/odeme/sonuc?status=basarisiz&id=" + payment.getId();
            }
        } catch (Exception e) {
            // Hata mesajı kart bilgisi içerebileceğinden loglanmaz, sadece paymentId loglanır
            log.error("Callback işleme hatası: paymentId={}", payment.getId());
            payment.setStatus(PaymentStatus.FAIL);
            paymentRepository.save(payment);
            return frontendUrl + "/odeme/sonuc?status=hata";
        }
    }

    /**
     * conversationId (Payment UUID'si) ile ödeme kaydını bulur.
     * Geçersiz UUID veya bulunamayan kayıt durumunda {@code null} döner.
     *
     * @param conversationId iyzico'nun callback'te geri gönderdiği conversationId
     * @return bulunan ödeme kaydı veya bulunamazsa {@code null}
     */
    private Payment findPaymentByConversationId(String conversationId) {
        try {
            return paymentRepository.findById(UUID.fromString(conversationId)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * iyzico'dan dönen 3DS ödeme yanıtının kart bilgisi içermeyen güvenli bir
     * özetini JSON olarak {@code providerResponse} alanına kaydeder.
     *
     * @param payment       güncellenecek ödeme kaydı
     * @param threedsPayment iyzico'dan dönen 3DS ödeme sonucu
     */
    private void saveProviderResponse(Payment payment, ThreedsPayment threedsPayment) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("paymentId", threedsPayment.getPaymentId());
            snapshot.put("status", threedsPayment.getStatus());
            snapshot.put("currency", threedsPayment.getCurrency());
            snapshot.put("paidPrice", threedsPayment.getPaidPrice());
            snapshot.put("errorCode", threedsPayment.getErrorCode());
            snapshot.put("errorMessage", threedsPayment.getErrorMessage());
            payment.setProviderResponse(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.warn("Ödeme sağlayıcı yanıtı kaydedilemedi: paymentId={}", payment.getId());
        }
    }

    // =========================================================================
    // Sorgulama
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID paymentId, User currentUser) {
        Payment payment = findAndAuthorize(paymentId, currentUser);
        return toResponse(payment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByAppointment(UUID appointmentId, User currentUser) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bu randevu için ödeme kaydı bulunamadı: " + appointmentId));

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bu ödeme kaydına erişim yetkiniz yok.");
        }
        return toResponse(payment);
    }

    // =========================================================================
    // İade
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Yalnızca {@code SUCCESS} durumundaki ödemeler iade edilebilir.
     * İade için iyzico'nun döndürdüğü {@code providerTransactionReference} gereklidir.
     */
    @Override
    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId, User currentUser) {
        Payment payment = findAndAuthorize(paymentId, currentUser);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("Yalnızca başarılı ödemeler iade edilebilir");
        }
        if (payment.getProviderTransactionReference() == null) {
            throw new BusinessException("İade için gerekli bilgi eksik");
        }

        CreateRefundRequest refundRequest = new CreateRefundRequest();
        refundRequest.setLocale(Locale.TR.getValue());
        refundRequest.setConversationId("iade-" + payment.getId());
        refundRequest.setPaymentTransactionId(payment.getProviderTransactionReference());
        refundRequest.setPrice(payment.getAmount());
        refundRequest.setCurrency(Currency.TRY.name());
        refundRequest.setIp("127.0.0.1");

        try {
            Refund refund = Refund.create(refundRequest, iyzicoOptions);

            if (!IYZICO_SUCCESS_STATUS.equals(refund.getStatus())) {
                log.warn("İade işlemi başarısız: paymentId={}", payment.getId());
                throw new BusinessException("İade işlemi başarısız");
            }

            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            log.info("İade başarılı: paymentId={}", payment.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Hata mesajı kart bilgisi içerebileceğinden loglanmaz, sadece paymentId loglanır
            log.error("İade işlemi sırasında hata oluştu: paymentId={}", payment.getId());
            throw new BusinessException("İade işlemi başarısız");
        }

        return toResponse(payment);
    }

    // =========================================================================
    // Yardımcı metodlar
    // =========================================================================

    /**
     * Ödeme kaydını bulur ve giriş yapmış kullanıcının bu kayda erişim
     * yetkisi olup olmadığını kontrol eder.
     *
     * @param paymentId   aranacak ödeme kaydının UUID'si
     * @param currentUser işlemi yapan kullanıcı
     * @return bulunan ve yetki kontrolünden geçen ödeme kaydı
     */
    private Payment findAndAuthorize(UUID paymentId, User currentUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı: " + paymentId));

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bu ödeme kaydına erişim yetkiniz yok.");
        }
        return payment;
    }

    /**
     * {@link Payment} entity'sini {@link PaymentResponse} record'una dönüştürür.
     *
     * @param payment dönüştürülecek ödeme entity'si
     * @return doldurulmuş yanıt record'u
     */
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    /**
     * Ad soyad metninden ilk kelimeyi (adı) ayıklar.
     *
     * @param fullName "Ad Soyad" formatında tam isim
     * @return ilk kelime (ad); boşsa boş string
     */
    private String ilkAd(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    /**
     * Ad soyad metninden son kelimeyi (soyadı) ayıklar.
     *
     * @param fullName "Ad Soyad" formatında tam isim
     * @return son kelime (soyad); boşsa boş string
     */
    private String sonSoyad(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

}
