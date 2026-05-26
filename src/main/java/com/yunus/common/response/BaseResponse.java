package com.yunus.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Tüm API yanıtları için standart response yapısı.
 * Başarılı ve hatalı durumlar için static factory metodlar sağlar.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private OffsetDateTime timestamp;

    public BaseResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public BaseResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = OffsetDateTime.now();
    }

    // Başarılı yanıt — sadece data
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, null, data);
    }

    // Başarılı yanıt — mesaj ve data
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(true, message, data);
    }

    // Başarılı yanıt — sadece mesaj
    public static <T> BaseResponse<T> success(String message) {
        return new BaseResponse<>(true, message, null);
    }

    // Hatalı yanıt — sadece mesaj
    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(false, message, null);
    }

    // Hatalı yanıt — mesaj ve data (validation hataları için)
    public static <T> BaseResponse<T> error(String message, T data) {
        return new BaseResponse<>(false, message, data);
    }
}
