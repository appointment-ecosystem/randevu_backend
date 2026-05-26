package com.yunus.review.repository;

import com.yunus.review.entity.Review;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme yorumları ve randevu başına tek değerlendirme kontrolü.
 */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByBusinessIdAndIsVisibleTrue(UUID businessId);

    Optional<Review> findByAppointmentId(UUID appointmentId);

}
