package com.yunus.payment.controller;

import com.yunus.common.response.BaseResponse;
import com.yunus.payment.dto.request.InitiatePaymentRequest;
import com.yunus.payment.dto.response.InitiatePaymentResponse;
import com.yunus.payment.dto.response.PaymentResponse;
import com.yunus.payment.service.PaymentService;
import com.yunus.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * iyzico ödeme işlemleri REST controller'ı.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "iyzico 3DS ödeme işlemleri")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Bir randevu için iyzico 3DS ödeme sürecini başlatır.
     *
     * <p>{@code POST /api/v1/payments/initiate} → HTTP 200
     *
     * @param userPrincipal giriş yapmış kullanıcı
     * @param request       randevu ve kart bilgilerini içeren istek
     * @param httpRequest   istemci IP adresini okumak için kullanılır
     * @return ödeme kaydının ID'si ve 3DS HTML içeriği
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/initiate")
    @Operation(summary = "Ödeme başlat",
               description = "Randevu için kapora tutarını hesaplar, ödeme kaydı oluşturur ve " +
                             "iyzico'dan dönen 3DS HTML içeriğini döner.")
    public ResponseEntity<BaseResponse<InitiatePaymentResponse>> initiatePayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody InitiatePaymentRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = resolveClientIp(httpRequest);
        InitiatePaymentResponse response = paymentService.initiatePayment(request, userPrincipal.getUser(), clientIp);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * iyzico'dan dönen 3DS callback isteğini işler.
     *
     * <p>Bu endpoint PUBLIC'tir; iyzico tarafından doğrudan çağrılır.
     * Yanıt olarak, üst pencereyi sonuç sayfasına yönlendiren bir HTML döner
     * (HTTP redirect kullanılmaz, çünkü çağrı bir iframe içinden gelir).
     *
     * <p>{@code POST /api/v1/payments/callback} → HTTP 200 (text/html)
     *
     * @param params   iyzico'nun gönderdiği form parametreleri (conversationId, mdStatus, vb.)
     * @param response üst pencereyi yönlendiren HTML'in yazılacağı yanıt
     */
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "iyzico 3DS callback",
               description = "iyzico'nun 3DS doğrulaması sonrası çağırdığı public endpoint. " +
                             "Ödeme ve randevu durumunu günceller, sonuç sayfasına yönlendiren HTML döner.")
    public void handleCallback(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String redirectUrl = paymentService.handleCallback(params);

        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body>
                <script>
                (function() {
                    var url = '%s';
                    if (window !== window.top) {
                        window.top.location.href = url;
                    } else {
                        window.location.href = url;
                    }
                })();
                </script>
                <p>Yönlendiriliyor...</p>
                </body>
                </html>
                """.formatted(redirectUrl);

        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(200);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(html);
        }
    }

    /**
     * Belirtilen ödemenin güncel durumunu döner.
     *
     * <p>{@code GET /api/v1/payments/{paymentId}/status} → HTTP 200
     *
     * @param userPrincipal giriş yapmış kullanıcı
     * @param paymentId     sorgulanacak ödemenin UUID'si
     * @return ödeme kaydının detayları
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{paymentId}/status")
    @Operation(summary = "Ödeme durumu",
               description = "Belirtilen ödemenin güncel durumunu döner.")
    public ResponseEntity<BaseResponse<PaymentResponse>> getPaymentStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID paymentId) {

        PaymentResponse response = paymentService.getPaymentStatus(paymentId, userPrincipal.getUser());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Başarılı bir ödemeyi iyzico üzerinden iade eder.
     *
     * <p>{@code POST /api/v1/payments/{paymentId}/cancel} → HTTP 200
     *
     * @param userPrincipal giriş yapmış kullanıcı
     * @param paymentId     iade edilecek ödemenin UUID'si
     * @return güncellenmiş ödeme kaydının detayları
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Ödemeyi iade et",
               description = "SUCCESS durumundaki bir ödemeyi iyzico üzerinden iade eder.")
    public ResponseEntity<BaseResponse<PaymentResponse>> cancelPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID paymentId) {

        PaymentResponse response = paymentService.cancelPayment(paymentId, userPrincipal.getUser());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen randevuya ait ödeme kaydını döner.
     *
     * <p>{@code GET /api/v1/payments/appointment/{appointmentId}} → HTTP 200
     *
     * @param userPrincipal giriş yapmış kullanıcı
     * @param appointmentId randevunun UUID'si
     * @return randevuya ait ödeme kaydının detayları
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Randevuya ait ödeme",
               description = "Belirtilen randevuya ait ödeme kaydını döner.")
    public ResponseEntity<BaseResponse<PaymentResponse>> getPaymentByAppointment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID appointmentId) {

        PaymentResponse response = paymentService.getPaymentByAppointment(appointmentId, userPrincipal.getUser());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * İstemcinin gerçek IP adresini belirler.
     * Reverse proxy arkasında çalışıldığında {@code X-Forwarded-For} başlığı önceliklidir.
     *
     * @param request gelen HTTP isteği
     * @return istemci IP adresi
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
