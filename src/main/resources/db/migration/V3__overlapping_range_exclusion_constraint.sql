-- V3: PostgreSQL Exclusion Constraint for Overlapping Appointments
--
-- Problem: V2'deki partial unique index (staff_id, start_time) yalnızca BİREBİR AYNI
--          başlangıç zamanına sahip çakışmaları engeller. Ancak farklı sürelerdeki
--          veya kısmi çakışan aralıkları (örneğin 10:00-11:00 randevusu varken 10:30-11:30)
--          engellemez.
--
-- Çözüm: PostgreSQL'in GiST tabanlı EXCLUDE kısıtı ve tstzrange aralık türünü kullanarak
--        çakışan zaman aralıklarını ([start_time, end_time)) DB seviyesinde garanti altına alıyoruz.
--
-- btree_gist eklentisi UUID ve TIMESTAMPTZ gibi standart tiplerin GiST indexinde
-- eşitlik (=) ve aralık çakışması (&&) ile birlikte kullanılabilmesi için gereklidir.

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Eğer V2'den kalan tekil başlangıç indexi varsa tutulabilir veya exclusion constraint
-- onu kapsadığı için çakışma durumunda exclusion devreye girer.
-- Aktif randevular (PENDING, CONFIRMED) ve atanmış personel (staff_id IS NOT NULL) için
-- çakışan zaman aralıklarını engelle:

ALTER TABLE appointments
ADD CONSTRAINT exclude_overlapping_appointments
EXCLUDE USING gist (
    staff_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED') AND staff_id IS NOT NULL);

-- Not: staff_id IS NULL (personelsiz/otomatik atanacak) randevular için exclusion constraint
-- PostgreSQL'in NULL ayrımı nedeniyle devreye girmez. Bu randevular işletme bazında
-- uygulama seviyesinde (SlotService / AppointmentService) korunur.
