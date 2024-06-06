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

import java.util.Set;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbAssetService {

    Asset save(Asset asset, User user) throws Exception;

    void delete(Asset asset, User user);

    Asset assignAssetToCustomer(Asset asset, Customer customer, User user) throws ThingsboardException;

    Asset unassignAssetFromCustomer(Asset asset, Customer customer, User user) throws ThingsboardException;

    Asset assignAssetToPublicCustomer(Asset asset, User user) throws ThingsboardException;

    Asset unassignAssetFromPublicCustomer(Asset asset, User user) throws ThingsboardException;

    Asset updateAssetCustomers(Asset asset, Set<CustomerId> customerIds, User user) throws ThingsboardException;

    Asset addAssetCustomers(Asset asset, Set<CustomerId> customerIds, User user) throws ThingsboardException;

    Asset removeAssetCustomers(Asset asset, Set<CustomerId> customerIds, User user) throws ThingsboardException;
    
    Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, User user) throws ThingsboardException;

    Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, User user) throws ThingsboardException;

}
