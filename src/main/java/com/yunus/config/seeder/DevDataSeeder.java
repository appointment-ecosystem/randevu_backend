package com.yunus.config.seeder;

import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessCategory;
import com.yunus.business.entity.BusinessStatus;
import com.yunus.business.entity.Service;
import com.yunus.business.entity.Staff;
import com.yunus.business.entity.WorkingHour;
import com.yunus.business.repository.BusinessCategoryRepository;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.business.repository.WorkingHourRepository;
import com.yunus.location.entity.City;
import com.yunus.location.entity.District;
import com.yunus.location.repository.CityRepository;
import com.yunus.location.repository.DistrictRepository;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dev ortamına özgü test verisi.
 * Sadece {@code spring.profiles.active=dev} ile ayağa kalkar;
 * veri zaten varsa (slug veya e-posta kontrolüyle) hiçbir şey yapmaz.
 *
 * <p>Oluşturulan veriler:</p>
 * <ul>
 *   <li>1 USER rolünde kullanıcı (test@test.com)</li>
 *   <li>3 APPROVED işletme — berber, kuaför, güzellik salonu</li>
 *   <li>Her işletmede 2 hizmet</li>
 *   <li>Her işletmede 1–2 personel</li>
 *   <li>Her işletme için Pazartesi–Cumartesi 09:00–19:00 çalışma saati</li>
 * </ul>
 */
@Component
public class DevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    // İşletme slug'ları — mevcut kontrol için kullanılır
    private static final String SLUG_BERBER    = "ahmet-berber-kadikoy";
    private static final String SLUG_KUAFOR    = "selin-kuafor-kadikoy";
    private static final String SLUG_GUZELLIK  = "moda-guzellik-salonu";

    private static final String TEST_USER_EMAIL = "test@test.com";
    private static final String TEST_USER_PHONE = "05551234567";

    private final UserRepository           userRepository;
    private final BusinessRepository       businessRepository;
    private final BusinessCategoryRepository categoryRepository;
    private final ServiceRepository        serviceRepository;
    private final StaffRepository          staffRepository;
    private final WorkingHourRepository    workingHourRepository;
    private final CityRepository           cityRepository;
    private final DistrictRepository       districtRepository;
    private final PasswordEncoder          passwordEncoder;

    public DevDataSeeder(UserRepository userRepository,
                         BusinessRepository businessRepository,
                         BusinessCategoryRepository categoryRepository,
                         ServiceRepository serviceRepository,
                         StaffRepository staffRepository,
                         WorkingHourRepository workingHourRepository,
                         CityRepository cityRepository,
                         DistrictRepository districtRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository        = userRepository;
        this.businessRepository    = businessRepository;
        this.categoryRepository    = categoryRepository;
        this.serviceRepository     = serviceRepository;
        this.staffRepository       = staffRepository;
        this.workingHourRepository = workingHourRepository;
        this.cityRepository        = cityRepository;
        this.districtRepository    = districtRepository;
        this.passwordEncoder       = passwordEncoder;
    }

    /**
     * Tüm dev seed verilerini oluşturur.
     * İlk çalıştırmada slug kontrolü ile idempotent çalışır.
     */
    @Transactional
    public void seed() {
        if (businessRepository.findBySlug(SLUG_BERBER).isPresent()) {
            log.debug("Dev seed data already exists, skipping.");
            return;
        }

        User testUser = seedTestUser();

        // Lokasyon: İstanbul → Kadıköy
        City istanbul = cityRepository.findAll().stream()
                .filter(c -> "İstanbul".equals(c.getName()))
                .findFirst()
                .orElse(null);

        District kadikoy = null;
        if (istanbul != null) {
            kadikoy = districtRepository.findByCityIdAndIsActiveTrue(istanbul.getId()).stream()
                    .filter(d -> "Kadıköy".equals(d.getName()))
                    .findFirst()
                    .orElse(null);
        }

        // Kategoriler (BusinessCategorySeeder çalışmış olmalı)
        BusinessCategory catBerber   = categoryRepository.findBySlug("berber").orElse(null);
        BusinessCategory catKuafor   = categoryRepository.findBySlug("kuafor").orElse(null);
        BusinessCategory catGuzellik = categoryRepository.findBySlug("guzellik-salonu").orElse(null);

        // ── İşletme 1: Berber ──────────────────────────────────────────────
        Business berber = createBusiness(
                testUser,
                "Ahmet Berber",
                SLUG_BERBER,
                "Kadıköy'ün en köklü berber salonu. Erkek saç ve sakal hizmetleri.",
                "05321111111",
                "Moda Caddesi No:12, Kadıköy",
                istanbul, kadikoy,
                40.9917, 29.0244,       // Kadıköy - Moda civarı
                catBerber != null ? Set.of(catBerber) : Set.of()
        );
        seedServices(berber,
                "Saç Kesimi",    "Profesyonel erkek saç kesimi",    30, new BigDecimal("150.00"), 1,
                "Sakal Düzeltme","Sakal şekillendirme ve düzeltme", 20, new BigDecimal("100.00"), 2);
        seedStaff(berber, "Ahmet Yılmaz", "Usta Berber", 1);
        seedStaff(berber, "Murat Kaya",   "Kalfa",        2);
        seedWorkingHours(berber);

        // ── İşletme 2: Kuaför ──────────────────────────────────────────────
        Business kuafor = createBusiness(
                testUser,
                "Selin Kuaför",
                SLUG_KUAFOR,
                "Kadın ve erkek saç tasarımı, boyama ve bakım hizmetleri.",
                "05322222222",
                "Bahariye Caddesi No:45, Kadıköy",
                istanbul, kadikoy,
                40.9877, 29.0289,       // Kadıköy - Bahariye civarı
                catKuafor != null ? Set.of(catKuafor) : Set.of()
        );
        seedServices(kuafor,
                "Saç Kesimi & Şekillendirme", "Makas + fön dahil", 60, new BigDecimal("350.00"), 1,
                "Saç Boyama",                 "Tekli renk boyama",  90, new BigDecimal("600.00"), 2);
        seedStaff(kuafor, "Selin Arslan", "Baş Stilist",  1);
        seedStaff(kuafor, "Deniz Çelik",  "Stilist",       2);
        seedWorkingHours(kuafor);

        // ── İşletme 3: Güzellik Salonu ──────────────────────────────────────
        Business guzellik = createBusiness(
                testUser,
                "Moda Güzellik Salonu",
                SLUG_GUZELLIK,
                "Cilt bakımı, makyaj ve SPA hizmetleri sunan premium güzellik merkezi.",
                "05323333333",
                "Moda Caddesi No:88, Kadıköy",
                istanbul, kadikoy,
                40.9898, 29.0210,       // Kadıköy - Moda sahili civarı
                catGuzellik != null ? Set.of(catGuzellik) : Set.of()
        );
        seedServices(guzellik,
                "Cilt Bakımı",    "Temizleme + nem maskesi + serum", 60, new BigDecimal("500.00"), 1,
                "Kalıcı Makyaj", "Kaş ve dudak kalıcı pigment",    120, new BigDecimal("1200.00"), 2);
        seedStaff(guzellik, "Ayşe Demir", "Estetisyen", 1);
        seedWorkingHours(guzellik);

        log.info("Dev seed data created: 1 test user, 3 businesses (berber/kuaför/güzellik), services, staff, working hours.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Yardımcı metotlar
    // ─────────────────────────────────────────────────────────────────────

    private User seedTestUser() {
        return userRepository.findByEmail(TEST_USER_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setFullName("Test Kullanıcı");
            u.setEmail(TEST_USER_EMAIL);
            u.setPhone(TEST_USER_PHONE);
            u.setPasswordHash(passwordEncoder.encode("Test1234"));
            u.setRole(UserRole.USER);
            u.setPhoneVerified(false);
            u.setIsActive(true);
            User saved = userRepository.save(u);
            log.info("Test user created: {}", TEST_USER_EMAIL);
            return saved;
        });
    }

    private Business createBusiness(User owner,
                                    String name,
                                    String slug,
                                    String description,
                                    String phone,
                                    String addressLine,
                                    City city,
                                    District district,
                                    double latitude,
                                    double longitude,
                                    Set<BusinessCategory> categories) {
        Business b = new Business();
        b.setOwner(owner);
        b.setName(name);
        b.setSlug(slug);
        b.setDescription(description);
        b.setPhone(phone);
        b.setAddressLine(addressLine);
        b.setCity(city);
        b.setDistrict(district);
        b.setLatitude(latitude);
        b.setLongitude(longitude);
        b.setStatus(BusinessStatus.APPROVED);
        b.setIsActive(true);
        b.setCategories(categories);
        return businessRepository.save(b);
    }

    /**
     * İki hizmet oluşturur. Parametreler çift olarak verilir:
     * (name1, desc1, dur1, price1, order1, name2, desc2, dur2, price2, order2)
     */
    private void seedServices(Business business,
                               String name1, String desc1, int dur1, BigDecimal price1, int order1,
                               String name2, String desc2, int dur2, BigDecimal price2, int order2) {
        serviceRepository.save(buildService(business, name1, desc1, dur1, price1, order1));
        serviceRepository.save(buildService(business, name2, desc2, dur2, price2, order2));
    }

    private Service buildService(Business business, String name, String desc,
                                  int durationMin, BigDecimal price, int sortOrder) {
        Service s = new Service();
        s.setBusiness(business);
        s.setName(name);
        s.setDescription(desc);
        s.setDurationMin(durationMin);
        s.setPrice(price);
        s.setCurrency("TRY");
        s.setIsActive(true);
        s.setSortOrder(sortOrder);
        return s;
    }

    private void seedStaff(Business business, String fullName, String title, int sortOrder) {
        Staff st = new Staff();
        st.setBusiness(business);
        st.setFullName(fullName);
        st.setTitle(title);
        st.setIsActive(true);
        st.setSortOrder(sortOrder);
        staffRepository.save(st);
    }

    /**
     * İşletme geneli Pazartesi (1) – Cumartesi (6) arası 09:00–19:00 ekler.
     * Pazar (7) kapalı olarak eklenmez (sadece açık günler eklenir).
     */
    private void seedWorkingHours(Business business) {
        LocalTime open  = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(19, 0);

        for (int day = 1; day <= 6; day++) {   // 1=Pazartesi … 6=Cumartesi
            WorkingHour wh = new WorkingHour();
            wh.setBusiness(business);
            wh.setStaff(null);                  // işletme geneli
            wh.setDayOfWeek(day);
            wh.setOpenTime(open);
            wh.setCloseTime(close);
            wh.setIsClosed(false);
            workingHourRepository.save(wh);
        }
    }
}
