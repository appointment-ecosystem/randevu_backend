package com.yunus.config.seeder;

import com.yunus.business.entity.BusinessCategory;
import com.yunus.business.repository.BusinessCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * İşletme kategorileri için başlangıç verisi.
 * Kayıt zaten varsa tekrar eklemez.
 */
@Component
public class BusinessCategorySeeder {

    private static final Logger log = LoggerFactory.getLogger(BusinessCategorySeeder.class);

    private final BusinessCategoryRepository businessCategoryRepository;

    public BusinessCategorySeeder(BusinessCategoryRepository businessCategoryRepository) {
        this.businessCategoryRepository = businessCategoryRepository;
    }

    @Transactional
    public void seed() {
        if (businessCategoryRepository.count() > 0) {
            log.debug("Business category data already exists, skipping seed.");
            return;
        }

        seedCategory("Berber", "berber", 1);
        seedCategory("Kuaför", "kuafor", 2);
        seedCategory("Güzellik Salonu", "guzellik-salonu", 3);
        seedCategory("Tırnak Stüdyosu", "tirnak-studyosu", 4);
        seedCategory("Dövme Stüdyosu", "dovme-studyosu", 5);

        log.info("Business category seed data created: 5 categories.");
    }

    private void seedCategory(String name, String slug, int sortOrder) {
        BusinessCategory category = new BusinessCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setSortOrder(sortOrder);
        category.setIsActive(true);
        businessCategoryRepository.save(category);
    }
}
