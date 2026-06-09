package com.yunus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Genel yerel hizmet ve randevu ekosistemi — Spring Boot giriş noktası.
 * Alt paketlerdeki entity, repository ve ileride eklenecek service/controller bileşenlerini tarar.
 * @ConfigurationPropertiesScan ile tüm @ConfigurationProperties sınıflarını otomatik tarar.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class RandevuApplication {

    public static void main(String[] args) {
        SpringApplication.run(RandevuApplication.class, args);
    }

}
