package com.yunus.sms.service;

import com.yunus.sms.config.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Netgsm API entegrasyonu ile SMS gönderimi sağlayan servis.
 * "demo" veya "test" kullanıcı adlarında SMS göndermek yerine yalnızca log yazar.
 * Diğer durumlarda HTTP Client üzerinden Netgsm API'sine GET isteği atar.
 */
@Service
public class NetgsmSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(NetgsmSmsService.class);

    private final SmsProperties smsProperties;
    private final HttpClient httpClient;

    public NetgsmSmsService(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public void sendSms(String phone, String message) {
        String username = smsProperties.username();

        if ("demo".equalsIgnoreCase(username) || "test".equalsIgnoreCase(username)) {
            log.info("[DEV/TEST MODE - SMS LOG] To: {}, Message: '{}'", phone, message);
            return;
        }

        try {
            // Netgsm GET API formatına uygun URL parametreleri oluşturulur
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = String.format("%s?usercode=%s&password=%s&gsmno=%s&message=%s&msgheader=%s",
                    smsProperties.apiUrl(),
                    smsProperties.username(),
                    smsProperties.password(),
                    phone,
                    encodedMessage,
                    smsProperties.header()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            log.info("Sending SMS to {} via Netgsm", phone);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Netgsm başarılı gönderimde "00" veya benzeri bir kod döner
                if (body != null && body.startsWith("00")) {
                    log.info("SMS successfully sent to: {}. Netgsm code: {}", phone, body);
                } else {
                    log.error("SMS sending failed with Netgsm response: {}", body);
                }
            } else {
                log.error("SMS sending failed. HTTP Status: {}", response.statusCode());
            }

        } catch (Exception ex) {
            log.error("Exception occurred while sending SMS to: {}", phone, ex);
        }
    }
}
