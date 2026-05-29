package com.yunus.business.service;

import com.yunus.business.dto.BusinessResponse;
import com.yunus.business.dto.BusinessStatusResponse;
import com.yunus.business.dto.CreateBusinessRequest;
import com.yunus.business.dto.UpdateBusinessRequest;
import java.util.List;
import java.util.UUID;

/**
 * İşletme oluşturma ve yönetim operasyonları.
 */
public interface BusinessService {

    BusinessResponse createBusiness(UUID ownerId, CreateBusinessRequest request);

    BusinessResponse updateBusiness(UUID ownerId, UUID businessId, UpdateBusinessRequest request);

    BusinessResponse getBusiness(UUID businessId);

    BusinessStatusResponse getMyBusinessStatus(UUID ownerId, UUID businessId);

    List<BusinessResponse> getMyBusinesses(UUID ownerId);
}

