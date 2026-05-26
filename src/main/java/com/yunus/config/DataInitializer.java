package com.yunus.config;

import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Uygulama başlangıcında çalışan veri başlatıcı.
 * Veritabanında ADMIN kullanıcı yoksa varsayılan bir admin hesabı oluşturur.
 * Üretim ortamında şifreyi environment variable'dan okuyun ve değiştirin.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_PHONE = "05000000000";
    private static final String ADMIN_DEFAULT_PASSWORD = "Admin@12345";
    private static final String ADMIN_FULL_NAME = "System Admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
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
