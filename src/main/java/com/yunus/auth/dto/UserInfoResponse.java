package com.yunus.auth.dto;

import com.yunus.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * Giriş yapmış kullanıcının profil bilgilerini dönen DTO sınıfı.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private UUID id;
    private String fullName;
    private String phone;
    private String email;
    private UserRole role;
    private Boolean phoneVerified;
    private String profilePhotoUrl;
}
