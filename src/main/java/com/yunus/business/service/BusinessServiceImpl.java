package com.yunus.business.service;

import com.yunus.business.config.BusinessProperties;
import com.yunus.business.dto.BusinessResponse;
import com.yunus.business.dto.BusinessStatusResponse;
import com.yunus.business.dto.CreateBusinessRequest;
import com.yunus.business.dto.UpdateBusinessRequest;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessCategory;
import com.yunus.business.entity.BusinessStatus;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessCategoryRepository;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.location.entity.City;
import com.yunus.location.entity.District;
import com.yunus.location.entity.Neighborhood;
import com.yunus.location.repository.CityRepository;
import com.yunus.location.repository.DistrictRepository;
import com.yunus.location.repository.NeighborhoodRepository;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import com.yunus.webhook.WebhookEvent;
import com.yunus.webhook.WebhookService;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * İşletme oluşturma ve güncelleme işlemlerini yönetir.
 */
@Service
@Transactional
public class BusinessServiceImpl implements BusinessService {

    private static final Logger log = LoggerFactory.getLogger(BusinessServiceImpl.class);

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository businessCategoryRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final BusinessProperties businessProperties;
    private final WebhookService webhookService;

    public BusinessServiceImpl(BusinessRepository businessRepository,
                               BusinessCategoryRepository businessCategoryRepository,
                               CityRepository cityRepository,
                               DistrictRepository districtRepository,
                               NeighborhoodRepository neighborhoodRepository,
                               UserRepository userRepository,
                               StaffRepository staffRepository,
                               BusinessProperties businessProperties,
                               WebhookService webhookService) {
        this.businessRepository = businessRepository;
        this.businessCategoryRepository = businessCategoryRepository;
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.businessProperties = businessProperties;
        this.webhookService = webhookService;
    }

    @Override
    public BusinessResponse createBusiness(UUID ownerId, CreateBusinessRequest request) {
        User owner = getActiveUser(ownerId);
        if (owner.getRole() != UserRole.BUSINESS_OWNER) {
            throw new ForbiddenException("Bu işlem için işletme sahibi rolü gereklidir");
        }

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("Şehir", "id", request.cityId()));
        District district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new ResourceNotFoundException("İlçe", "id", request.districtId()));
        Neighborhood neighborhood = neighborhoodRepository.findById(request.neighborhoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Mahalle", "id", request.neighborhoodId()));

        Set<BusinessCategory> categories = resolveCategories(request.categoryIds());

        Business business = new Business();
        business.setOwner(owner);
        business.setName(request.name().trim());
        business.setDescription(request.description());
        business.setPhone(request.phone());
        business.setEmail(request.email());
        business.setWebsite(request.website());
        business.setCity(city);
        business.setDistrict(district);
        business.setNeighborhood(neighborhood);
        business.setAddressLine(request.addressLine());
        business.setLatitude(request.latitude());
        business.setLongitude(request.longitude());
        business.setCategories(categories);

        BusinessStatus status = businessProperties.autoApprove() ? BusinessStatus.APPROVED : BusinessStatus.PENDING;
        business.setStatus(status);

        String slug = generateUniqueSlug(request.name());
        business.setSlug(slug);

        Business saved = businessRepository.save(business);
        log.info("Business created with id {} and slug {} by owner {}", saved.getId(), saved.getSlug(), ownerId);

        webhookService.sendWebhook(WebhookEvent.BUSINESS_REGISTERED, Map.of(
                "businessId", saved.getId().toString(),
                "businessName", saved.getName(),
                "ownerEmail", owner.getEmail() != null ? owner.getEmail() : "",
                "createdAt", saved.getCreatedAt().toString()
        ));

        return toBusinessResponse(saved);
    }

    @Override
    public BusinessResponse updateBusiness(UUID ownerId, UUID businessId, UpdateBusinessRequest request) {
        Business business = getOwnedBusiness(ownerId, businessId);

        if (request.name() != null && !request.name().isBlank()) {
            String newName = request.name().trim();
            if (!newName.equals(business.getName())) {
                business.setName(newName);
                String newSlug = generateUniqueSlug(newName);
                business.setSlug(newSlug);
            }
        }
        if (request.description() != null) {
            business.setDescription(request.description());
        }
        if (request.phone() != null) {
            business.setPhone(request.phone());
        }
        if (request.email() != null) {
            business.setEmail(request.email());
        }
        if (request.website() != null) {
            business.setWebsite(request.website());
        }
        if (request.cityId() != null) {
            City city = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Şehir", "id", request.cityId()));
            business.setCity(city);
        }
        if (request.districtId() != null) {
            District district = districtRepository.findById(request.districtId())
                    .orElseThrow(() -> new ResourceNotFoundException("İlçe", "id", request.districtId()));
            business.setDistrict(district);
        }
        if (request.neighborhoodId() != null) {
            Neighborhood neighborhood = neighborhoodRepository.findById(request.neighborhoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mahalle", "id", request.neighborhoodId()));
            business.setNeighborhood(neighborhood);
        }
        if (request.addressLine() != null) {
            business.setAddressLine(request.addressLine());
        }
        if (request.latitude() != null) {
            business.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            business.setLongitude(request.longitude());
        }
        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            Set<BusinessCategory> categories = resolveCategories(request.categoryIds());
            business.setCategories(categories);
        }

        Business saved = businessRepository.save(business);
        return toBusinessResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessResponse getBusiness(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", businessId));
        return toBusinessResponse(business);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessStatusResponse getMyBusinessStatus(UUID ownerId, UUID businessId) {
        Business business = getOwnedBusiness(ownerId, businessId);
        return new BusinessStatusResponse(
                business.getId(),
                business.getName(),
                business.getStatus(),
                business.getRejectionReason()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessResponse> getMyBusinesses(UUID ownerId) {
        getActiveUser(ownerId); // owner kontrolü
        List<Business> ownedBusinesses = businessRepository.findByOwnerId(ownerId);
        if (!ownedBusinesses.isEmpty()) {
            return ownedBusinesses.stream()
                    .map(this::toBusinessResponse)
                    .toList();
        }

        // Owner değilse, BUSINESS_EMPLOYEE olarak bağlı olduğu personel kaydı üzerinden işletmeyi bul
        return staffRepository.findByUserIdAndIsActiveTrue(ownerId)
                .map(Staff::getBusiness)
                .map(this::toBusinessResponse)
                .map(List::of)
                .orElseGet(List::of);
    }

    private User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResourceNotFoundException("Kullanıcı", "id", userId);
        }
        return user;
    }

    private Business getOwnedBusiness(UUID ownerId, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", businessId));
        if (!business.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Bu işletme size ait değil");
        }
        return business;
    }

    private Set<BusinessCategory> resolveCategories(List<UUID> categoryIds) {
        List<BusinessCategory> categories = businessCategoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new BusinessException("Geçersiz kategori seçimi");
        }
        return Set.copyOf(categories);
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = toSlug(name);
        String candidate = baseSlug;
        int counter = 1;
        while (businessRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + counter;
            counter++;
            if (counter > 50) {
                throw new BusinessException("İşletme için benzersiz kısa ad üretilemedi, lütfen farklı bir isim deneyin");
            }
        }
        return candidate;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
        if (slug.isBlank()) {
            throw new BusinessException("Geçersiz işletme adı");
        }
        return slug;
    }

    private BusinessResponse toBusinessResponse(Business business) {
        City city = business.getCity();
        District district = business.getDistrict();
        Neighborhood neighborhood = business.getNeighborhood();

        List<String> categoryNames = business.getCategories().stream()
                .map(BusinessCategory::getName)
                .collect(Collectors.toList());

        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getSlug(),
                business.getDescription(),
                business.getPhone(),
                business.getEmail(),
                business.getWebsite(),
                city != null ? city.getId() : null,
                city != null ? city.getName() : null,
                district != null ? district.getId() : null,
                district != null ? district.getName() : null,
                neighborhood != null ? neighborhood.getId() : null,
                neighborhood != null ? neighborhood.getName() : null,
                business.getAddressLine(),
                business.getLatitude(),
                business.getLongitude(),
                business.getStatus(),
                business.getRejectionReason(),
                business.getIsActive(),
                business.getOwner().getFullName(),
                categoryNames,
                business.getCreatedAt(),
                business.getUpdatedAt()
        );
    }
}

