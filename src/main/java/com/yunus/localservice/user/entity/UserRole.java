package com.yunus.localservice.user.entity;

/**
 * Kullanıcının sistemdeki yetki ve rol tipini belirler.
 * Veritabanında VARCHAR olarak saklanır (EnumType.STRING).
 */
public enum UserRole {
    USER,
    BUSINESS_OWNER,
    BUSINESS_EMPLOYEE,
    ADMIN
}
