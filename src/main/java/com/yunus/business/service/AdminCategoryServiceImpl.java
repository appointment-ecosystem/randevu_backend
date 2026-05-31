package com.yunus.business.service;

import com.yunus.business.dto.AdminCategoryCreateRequest;
import com.yunus.business.dto.AdminCategoryResponse;
import com.yunus.business.dto.AdminCategoryUpdateRequest;
import com.yunus.business.entity.BusinessCategory;
import com.yunus.business.repository.BusinessCategoryRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.exception.ErrorType;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin paneli kategori yönetimi servis arayüzünün (AdminCategoryService) iş mantığı uygulaması.
 * Çakışma kontrolü, kaynak doğrulama ve entity-DTO dönüşüm işlemlerini yönetir.
 */
@Service
@Transactional
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private static final Logger log = LoggerFactory.getLogger(AdminCategoryServiceImpl.class);

    private final BusinessCategoryRepository categoryRepository;

    /**
     * Bağımlılıkları constructor üzerinden enjekte eden yapıcı metot.
     *
     * @param categoryRepository İşletme kategorisi veri erişim repository nesnesi
     */
    public AdminCategoryServiceImpl(BusinessCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Tüm kategorileri sortOrder değerine göre artan sırada döner.
     * Salt okunur işlem olduğundan readOnly transaction kullanılır.
     *
     * @return Sıralı AdminCategoryResponse listesi
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> getAllCategories() {
        log.info("Admin tüm kategorileri listeleme isteği");
        return categoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Yeni kategori oluşturur.
     * Aynı name veya slug'a sahip kategori mevcutsa HTTP 409 CONFLICT fırlatır.
     *
     * @param request Oluşturulacak kategoriye ait bilgiler
     * @return Oluşturulan kategorinin yanıt DTO'su
     */
    @Override
    public AdminCategoryResponse createCategory(AdminCategoryCreateRequest request) {
        log.info("Yeni kategori oluşturma isteği — name: {}, slug: {}", request.name(), request.slug());

        // Aynı name ile kayıt var mı kontrol et
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new BusinessException(ErrorType.CONFLICT, "Bu isim zaten kullanılıyor: " + request.name());
        }

        // Aynı slug ile kayıt var mı kontrol et
        if (categoryRepository.findBySlug(request.slug()).isPresent()) {
            throw new BusinessException(ErrorType.CONFLICT, "Bu slug zaten kullanılıyor: " + request.slug());
        }

        // Yeni entity oluştur ve kaydet
        BusinessCategory category = new BusinessCategory();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setIconUrl(request.iconUrl());
        category.setIsActive(true);
        category.setSortOrder(request.sortOrder());

        BusinessCategory saved = categoryRepository.save(category);
        log.info("Kategori başarıyla oluşturuldu — id: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * Mevcut kategoriyi günceller.
     * Kategori bulunamazsa HTTP 404, başka kayıtta name/slug çakışması varsa HTTP 409 fırlatır.
     *
     * @param id      Güncellenecek kategorinin kimliği
     * @param request Yeni değerleri içeren güncelleme DTO'su
     * @return Güncellenmiş kategorinin yanıt DTO'su
     */
    @Override
    public AdminCategoryResponse updateCategory(UUID id, AdminCategoryUpdateRequest request) {
        log.info("Kategori güncelleme isteği — id: {}", id);

        // Kategori varlık kontrolü
        BusinessCategory category = findCategoryById(id);

        // Aynı name'e sahip başka kayıt var mı kontrol et (kendi id'si hariç)
        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException(ErrorType.CONFLICT, "Bu isim başka bir kategoride zaten kullanılıyor: " + request.name());
        }

        // Aynı slug'a sahip başka kayıt var mı kontrol et (kendi id'si hariç)
        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new BusinessException(ErrorType.CONFLICT, "Bu slug başka bir kategoride zaten kullanılıyor: " + request.slug());
        }

        // Alanları güncelle
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setIconUrl(request.iconUrl());

        // sortOrder null gönderilmişse mevcut değer korunur
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }

        BusinessCategory updated = categoryRepository.save(category);
        log.info("Kategori başarıyla güncellendi — id: {}", updated.getId());

        return toResponse(updated);
    }

    /**
     * Belirtilen kategoriyi pasife alır (isActive = false).
     * Kategori bulunamazsa HTTP 404 fırlatır.
     *
     * @param id Pasife alınacak kategorinin kimliği
     */
    @Override
    public void deactivateCategory(UUID id) {
        log.info("Kategori pasife alma isteği — id: {}", id);
        BusinessCategory category = findCategoryById(id);
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Kategori pasife alındı — id: {}", id);
    }

    /**
     * Belirtilen kategoriyi aktif hale getirir (isActive = true).
     * Kategori bulunamazsa HTTP 404 fırlatır.
     *
     * @param id Aktifleştirilecek kategorinin kimliği
     */
    @Override
    public void activateCategory(UUID id) {
        log.info("Kategori aktifleştirme isteği — id: {}", id);
        BusinessCategory category = findCategoryById(id);
        category.setIsActive(true);
        categoryRepository.save(category);
        log.info("Kategori aktifleştirildi — id: {}", id);
    }

    // ─── Yardımcı metodlar ────────────────────────────────────────────────────

    /**
     * Kimlik bilgisine göre kategoriyi veri tabanından sorgular.
     * Bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id Sorgulanacak kategorinin kimliği
     * @return Bulunan BusinessCategory entity nesnesi
     */
    private BusinessCategory findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorType.CATEGORY_NOT_FOUND, "Kategori bulunamadı: " + id));
    }

    /**
     * BusinessCategory entity nesnesini AdminCategoryResponse DTO'suna dönüştürür.
     *
     * @param category Dönüştürülecek entity nesnesi
     * @return Yanıt DTO'su
     */
    private AdminCategoryResponse toResponse(BusinessCategory category) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIconUrl(),
                category.getIsActive(),
                category.getSortOrder(),
                category.getCreatedAt()
        );
    }
}
