package com.yunus.business.service;

import com.yunus.business.dto.BulkWorkingHourRequest;
import com.yunus.business.dto.WorkingHourRequest;
import com.yunus.business.dto.WorkingHourResponse;
import java.util.List;
import java.util.UUID;

/**
 * İşletme ve personel çalışma saatlerini yönetmek için servis sözleşmesi.
 * Toplu set (7 günü tek seferde) ve tekil güncelleme operasyonlarını tanımlar.
 */
public interface WorkingHourService {

    /**
     * İşletmenin genel çalışma saatlerini dayOfWeek'e göre sıralı döner.
     */
    List<WorkingHourResponse> getBusinessHours(UUID businessId);

    /**
     * Belirtilen personelin çalışma saatlerini dayOfWeek'e göre sıralı döner.
     */
    List<WorkingHourResponse> getStaffHours(UUID businessId, UUID staffId);

    /**
     * İşletme geneli çalışma saatlerini tamamen yeniden set eder.
     * Mevcut kayıtlar silinir; yenileri oluşturulur.
     */
    List<WorkingHourResponse> setBusinessHours(UUID businessId, BulkWorkingHourRequest request);

    /**
     * Personel çalışma saatlerini tamamen yeniden set eder.
     * Mevcut kayıtlar silinir; yenileri oluşturulur.
     */
    List<WorkingHourResponse> setStaffHours(UUID businessId, UUID staffId, BulkWorkingHourRequest request);

    /**
     * Tek bir günün çalışma saatini günceller.
     */
    WorkingHourResponse updateOneDay(UUID businessId, UUID workingHourId, WorkingHourRequest request);
}
