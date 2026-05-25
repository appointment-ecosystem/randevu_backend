package com.yunus.localservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Genel yerel hizmet ve randevu ekosistemi — Spring Boot giriş noktası.
 * Alt paketlerdeki entity, repository ve ileride eklenecek service/controller bileşenlerini tarar.
 */
@SpringBootApplication
public class RandevuApplication {

    public static void main(String[] args) {
        SpringApplication.run(RandevuApplication.class, args);
    }

}
