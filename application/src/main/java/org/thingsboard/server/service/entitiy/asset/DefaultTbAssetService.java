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
package org.thingsboard.server.service.entitiy.asset;

import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    private final AssetService assetService;

    @Override
    public Asset save(Asset asset, User user) throws Exception {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));
            autoCommit(user, savedAsset.getId());
            logEntityActionService.logEntityAction(tenantId, savedAsset.getId(), savedAsset, asset.getCustomerId(),
                    actionType, user);
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), asset, actionType, user, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void delete(Asset asset, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            assetService.deleteAsset(tenantId, assetId);
            logEntityActionService.logEntityAction(tenantId, assetId, asset, asset.getCustomerId(), actionType, user, assetId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e,
                    assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToCustomer(Asset asset, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        CustomerId customerId = customer.getId();
        AssetId assetId = asset.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
            logEntityActionService.logEntityAction(tenantId, assetId, savedAsset, customerId, actionType,
                    user, assetId.toString(), customerId.toString(), customer.getName());
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), customerId.toString());
            throw e;
        }
    }


    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToEdge(tenantId, assetId, edgeId));
            logEntityActionService.logEntityAction(tenantId, assetId, savedAsset, savedAsset.getCustomerId(),
                    actionType, user, assetId.toString(), edgeId.toString(), edge.getName());
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        AssetId assetId = asset.getId();
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromEdge(tenantId, assetId, edgeId));
            logEntityActionService.logEntityAction(tenantId, assetId, asset, asset.getCustomerId(),
                    actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }


    @Override
    public Asset updateAssetCustomers(Asset asset, Set<CustomerId> customerIds, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            Set<CustomerId> addedCustomerIds = new HashSet<>();
            Set<CustomerId> removedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (!asset.isAssignedToCustomer(customerId)) {
                    addedCustomerIds.add(customerId);
                }
            }

            Set<ShortCustomerInfo> assignedCustomers = asset.getAssignedCustomers();
            if (assignedCustomers != null) {
                for (ShortCustomerInfo customerInfo : assignedCustomers) {
                    if (!customerIds.contains(customerInfo.getCustomerId())) {
                        removedCustomerIds.add(customerInfo.getCustomerId());
                    }
                }
            }

            if (addedCustomerIds.isEmpty() && removedCustomerIds.isEmpty()) {
                return asset;
            } else {
                Asset savedAsset = null;
                for (CustomerId customerId : addedCustomerIds) {
                    savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
                    ShortCustomerInfo customerInfo = savedAsset.getAssignedCustomerInfo(customerId);
                    logEntityActionService.logEntityAction(tenantId, savedAsset.getId(), savedAsset, customerId,
                            actionType, user, assetId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
                for (CustomerId customerId : removedCustomerIds) {
                    ShortCustomerInfo customerInfo = asset.getAssignedCustomerInfo(customerId);
                    savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId, customerId));
                    logEntityActionService.logEntityAction(tenantId, savedAsset.getId(), savedAsset, customerId,
                            actionType, user, assetId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                return savedAsset;
            }
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset addAssetCustomers(Asset asset, Set<CustomerId> customerIds, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            Set<CustomerId> addedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (!asset.isAssignedToCustomer(customerId)) {
                    addedCustomerIds.add(customerId);
                }
            }
            if (addedCustomerIds.isEmpty()) {
                return asset;
            } else {
                Asset savedAsset = null;
                for (CustomerId customerId : addedCustomerIds) {
                    savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
                    ShortCustomerInfo customerInfo = savedAsset.getAssignedCustomerInfo(customerId);
                    logEntityActionService.logEntityAction(tenantId, assetId, savedAsset, customerId,
                            actionType, user, assetId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                return savedAsset;
            }
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset removeAssetCustomers(Asset asset, Set<CustomerId> customerIds, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            Set<CustomerId> removedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (asset.isAssignedToCustomer(customerId)) {
                    removedCustomerIds.add(customerId);
                }
            }
            if (removedCustomerIds.isEmpty()) {
                return asset;
            } else {
                Asset savedAsset = null;
                for (CustomerId customerId : removedCustomerIds) {
                    ShortCustomerInfo customerInfo = asset.getAssignedCustomerInfo(customerId);
                    savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId, customerId));
                    logEntityActionService.logEntityAction(tenantId, assetId, savedAsset, customerId,
                            actionType, user, assetId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                return savedAsset;
            }
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetFromCustomer(Asset asset, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId, customer.getId()));
            logEntityActionService.logEntityAction(tenantId, assetId, savedAsset, customer.getId(),
                    actionType, user, assetId.toString(), customer.getId().toString(), customer.getName());
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToPublicCustomer(Asset asset, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, publicCustomer.getId()));
            logEntityActionService.logEntityAction(tenantId, assetId, savedAsset, publicCustomer.getId(),
                    actionType, user, assetId.toString(), publicCustomer.getId().toString(), publicCustomer.getName());
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetFromPublicCustomer(Asset asset, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId, publicCustomer.getId()));
            logEntityActionService.logEntityAction(tenantId, assetId, asset, publicCustomer.getId(), actionType,
                    user, assetId.toString(), publicCustomer.getId().toString(), publicCustomer.getName());
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }
}
