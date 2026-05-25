package com.yunus.localservice.business.entity;

import com.yunus.localservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * İşletme veya personele özel haftalık çalışma saatleri.
 * staff null ise işletme geneli; dolu ise o personele özel saatlerdir.
 */
@Entity
@Table(
        name = "working_hours",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "staff_id", "day_of_week"})
)
@Getter
@Setter
@NoArgsConstructor
public class WorkingHour extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // null = işletme geneli saatler; dolu = sadece bu personel
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    // 1 = Pazartesi, 7 = Pazar
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    // true ise o gün kapalı; open/close saatleri yok sayılabilir
    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

}
