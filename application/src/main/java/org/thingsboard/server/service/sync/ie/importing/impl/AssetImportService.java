/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetImportService extends BaseEntityImportService<AssetId, Asset, EntityExportData<Asset>> {

    private final AssetService assetService;

    @Override
    protected void setOwner(TenantId tenantId, Asset asset, IdProvider idProvider) {
        asset.setTenantId(tenantId);
        asset.setCustomerId(idProvider.getInternalId(asset.getCustomerId()));
        asset.setAssignedCustomers(new HashSet<>());
    }

    @Override
    protected Asset prepare(EntitiesImportCtx ctx, Asset asset, Asset old, EntityExportData<Asset> exportData, IdProvider idProvider) {
        asset.setAssetProfileId(idProvider.getInternalId(asset.getAssetProfileId()));
        return asset;
    }

    @Override
    protected Asset saveOrUpdate(EntitiesImportCtx ctx, Asset asset, EntityExportData<Asset> exportData, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();

        Set<ShortCustomerInfo> assignedCustomers = Optional.ofNullable(asset.getAssignedCustomers()).orElse(Collections.emptySet()).stream()
                .peek(customerInfo -> customerInfo.setCustomerId(idProvider.getInternalId(customerInfo.getCustomerId())))
                .collect(Collectors.toSet());

        if (asset.getId() == null) {
            asset.setAssignedCustomers(assignedCustomers);
            asset = assetService.saveAsset(asset);
            for (ShortCustomerInfo customerInfo : assignedCustomers) {
                asset = assetService.assignAssetToCustomer(tenantId, asset.getId(), customerInfo.getCustomerId());
            }
        } else {
            Set<CustomerId> existingAssignedCustomers = Optional.ofNullable(assetService.findAssetById(tenantId, asset.getId()).getAssignedCustomers())
                    .orElse(Collections.emptySet()).stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());
            Set<CustomerId> newAssignedCustomers = assignedCustomers.stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());

            Set<CustomerId> toUnassign = new HashSet<>(existingAssignedCustomers);
            toUnassign.removeAll(newAssignedCustomers);
            for (CustomerId customerId : toUnassign) {
                assignedCustomers = assetService.unassignAssetFromCustomer(tenantId, asset.getId(), customerId).getAssignedCustomers();
            }

            Set<CustomerId> toAssign = new HashSet<>(newAssignedCustomers);
            toAssign.removeAll(existingAssignedCustomers);
            for (CustomerId customerId : toAssign) {
                assignedCustomers = assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId).getAssignedCustomers();
            }
            asset.setAssignedCustomers(assignedCustomers);
            asset = assetService.saveAsset(asset);
        }
        return asset;
    }

    @Override
    protected Asset deepCopy(Asset asset) {
        return new Asset(asset);
    }

    @Override
    protected void cleanupForComparison(Asset e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
