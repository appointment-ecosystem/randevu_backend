-- V2: Partial unique index - slot çakışması koruması güçlendirildi
--
-- Problem: V1'deki (staff_id, start_time) unique index iptal/tamamlandı/gelmedi
--          durumlarında da geçerli; bu yüzden iptal edilmiş randevular yeniden
--          aynı slota alınmasını engelliyor.
--
-- Çözüm: Slotu "boşaltan" durumları (CANCELLED_BY_USER, CANCELLED_BY_BUSINESS,
--        COMPLETED, NO_SHOW, EXPIRED) index'ten dışarıda bırak.
--        Yalnızca aktif randevular (PENDING, CONFIRMED) çakışmayı engeller.
--
-- Neden bu durumlar dışarıda?
--   - CANCELLED_BY_USER / CANCELLED_BY_BUSINESS: Kullanıcı veya işletme iptal etti,
--     slot serbestleşti, başka randevu alınabilir.
--   - COMPLETED: Randevu bitti, slot geçmişte kaldı.
--   - NO_SHOW: Müşteri gelmedi, slot doluluk açısından artık önemli değil.
--   - EXPIRED: Sistem tarafından süresi doldu kabul edildi.
--
-- Eski V1 index'i düşür (varsa):
DROP INDEX IF EXISTS idx_appointments_staff_start_unique;

-- Yeni partial unique index: sadece aktif (PENDING veya CONFIRMED) randevularda
-- aynı personel + aynı başlangıç saatine izin verme.
CREATE UNIQUE INDEX idx_appointments_staff_start_active_unique
    ON appointments (staff_id, start_time)
    WHERE staff_id IS NOT NULL
      AND status IN ('PENDING', 'CONFIRMED');

-- Not: staff_id IS NULL (kişisiz randevular) bu index kapsamına GİRMEZ.
-- İşletme geneli çakışma koruması uygulama katmanında (SlotService) yönetilir.
