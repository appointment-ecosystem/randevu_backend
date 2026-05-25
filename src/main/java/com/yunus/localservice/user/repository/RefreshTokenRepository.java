package com.yunus.localservice.user.repository;

import com.yunus.localservice.user.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Refresh token arama ve oturum yenileme akışı için persistence erişimi.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

}
