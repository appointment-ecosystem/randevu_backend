package com.yunus.config;

import com.yunus.config.seeder.BusinessCategorySeeder;
import com.yunus.config.seeder.DevDataSeeder;
import com.yunus.config.seeder.LocationSeeder;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Uygulama başlangıcında çalışan veri başlatıcı.
 * <ul>
 *   <li>Konum ve kategori seed'leri her zaman çalışır.</li>
 *   <li>Admin kullanıcı seed'i her zaman çalışır.</li>
 *   <li>Dev test verisi yalnızca {@code dev} profili aktifken çalışır.</li>
 * </ul>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_PHONE            = "05000000000";
    private static final String ADMIN_DEFAULT_PASSWORD = "Admin@12345";
    private static final String ADMIN_FULL_NAME        = "System Admin";

    private final UserRepository          userRepository;
    private final PasswordEncoder         passwordEncoder;
    private final LocationSeeder          locationSeeder;
    private final BusinessCategorySeeder  businessCategorySeeder;
    private final DevDataSeeder           devDataSeeder;
    private final Environment             environment;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           LocationSeeder locationSeeder,
                           BusinessCategorySeeder businessCategorySeeder,
                           DevDataSeeder devDataSeeder,
                           Environment environment) {
        this.userRepository         = userRepository;
        this.passwordEncoder        = passwordEncoder;
        this.locationSeeder         = locationSeeder;
        this.businessCategorySeeder = businessCategorySeeder;
        this.devDataSeeder          = devDataSeeder;
        this.environment            = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        locationSeeder.seed();
        businessCategorySeeder.seed();
        seedAdminUser();

        // Dev test verisi: sadece "dev" profili aktifken çalışır
        boolean isDevProfile = java.util.Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (isDevProfile) {
            devDataSeeder.seed();
        }
    }

    /**
     * ADMIN rolünde varsayılan kullanıcı yoksa oluşturur.
     * Zaten varsa hiçbir şey yapmaz.
     */
    private void seedAdminUser() {
        if (userRepository.findByPhone(ADMIN_PHONE).isPresent()) {
            log.debug("Admin user already exists, skipping seed.");
            return;
        }

        User admin = new User();
        admin.setFullName(ADMIN_FULL_NAME);
        admin.setPhone(ADMIN_PHONE);
        admin.setEmail("admin@localservice.com");
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin.setPhoneVerified(true);
        admin.setIsActive(true);

        userRepository.save(admin);
        log.info("Default admin user created. Phone: {} — PLEASE CHANGE THE PASSWORD IN PRODUCTION!", ADMIN_PHONE);
    }
}
