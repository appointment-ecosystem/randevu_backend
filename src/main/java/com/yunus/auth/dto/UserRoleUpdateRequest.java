package com.yunus.auth.dto;

import com.yunus.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin tarafından kullanıcı rolü güncellemesi için request DTO sınıfı.
 */
@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotNull(message = "Yeni rol belirtilmelidir")
    private UserRole role;
}
