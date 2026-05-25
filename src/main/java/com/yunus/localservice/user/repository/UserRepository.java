package com.yunus.localservice.user.repository;

import com.yunus.localservice.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kullanıcı tablosu için temel CRUD ve kimlik doğrulama sorguları.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

}
