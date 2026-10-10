package com.yunus.business.validator;

import com.yunus.business.entity.Business;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * İşletme sahipliği ve işletmeye ait alt kaynakların (staff, service, photo, holiday, working hour)
 * sahipliğini ve aidiyetini doğrulayan merkezi bileşen.
 */
@Component
@RequiredArgsConstructor
public class BusinessOwnershipValidator {

    private final BusinessRepository businessRepository;
    private final CurrentUserService currentUserService;

    /**
     * İşletmeyi ID ile bulur; bulunamazsa ResourceNotFoundException fırlatır.
     *
     * @param businessId İşletme UUID'si
     * @return Bulunan Business entity
     */
    public Business getBusinessOrThrow(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", businessId));
    }

    /**
     * İşletmenin varlığını ve çağıran kullanıcının işletmenin sahibi olduğunu veya ADMIN olduğunu doğrular.
     * Sahip veya ADMIN değilse ForbiddenException fırlatır.
     *
     * @param businessId İşletme UUID'si
     * @return Doğrulanmış Business entity
     */
    public Business validateOwnership(UUID businessId) {
        Business business = getBusinessOrThrow(businessId);
        User currentUser = currentUserService.getCurrentUser();

        boolean isOwner = business.getOwner() != null && business.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Bu işletme üzerinde işlem yapma yetkiniz yok.");
        }
        return business;
    }
}
