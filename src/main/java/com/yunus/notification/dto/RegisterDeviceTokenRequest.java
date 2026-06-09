package com.yunus.notification.dto;

import com.yunus.notification.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceTokenRequest {

    @NotBlank
    private String token;

    @NotNull
    private DevicePlatform platform;

}
