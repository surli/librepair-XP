/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.api;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.apache.syncope.core.provisioning.api.pushpull.ReconciliationFilterBuilder;

/**
 * Entry point for making requests on underlying connector bundles.
 */
public interface Connector {

    /**
     * Authenticate user on a connector instance.
     *
     * @param username the name based credential for authentication
     * @param password the password based credential for authentication
     * @param options ConnId's OperationOptions
     * @return Uid of the account that was used to authenticate
     */
    Uid authenticate(String username, String password, OperationOptions options);

    /**
     * Create user / group on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param attrs attributes for creation
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return Uid for created object
     */
    Uid create(
            ObjectClass objectClass,
            Set<Attribute> attrs,
            OperationOptions options,
            Boolean[] propagationAttempted);

    /**
     * Update user / group on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param uid user to be updated
     * @param attrs attributes for update
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return Uid for updated object
     */
    Uid update(
            ObjectClass objectClass,
            Uid uid,
            Set<Attribute> attrs,
            OperationOptions options,
            Boolean[] propagationAttempted);

    /**
     * Delete user / group on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param uid user to be deleted
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if deletion is actually performed (based on connector instance's capabilities)
     */
    void delete(
            ObjectClass objectClass,
            Uid uid,
            OperationOptions options,
            Boolean[] propagationAttempted);

    /**
     * Fetches all remote objects (for use during full reconciliation).
     *
     * @param objectClass ConnId's object class.
     * @param handler to be used to handle deltas.
     * @param options ConnId's OperationOptions.
     */
    void fullReconciliation(ObjectClass objectClass, SyncResultsHandler handler, OperationOptions options);

    /**
     * Fetches remote objects (for use during filtered reconciliation).
     *
     * @param objectClass ConnId's object class.
     * @param filterBuilder reconciliation filter builder
     * @param handler to be used to handle deltas.
     * @param options ConnId's OperationOptions.
     */
    void filteredReconciliation(
            ObjectClass objectClass,
            ReconciliationFilterBuilder filterBuilder,
            SyncResultsHandler handler,
            OperationOptions options);

    /**
     * Sync remote objects from a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param token to be passed to the underlying connector
     * @param handler to be used to handle deltas
     * @param options ConnId's OperationOptions
     */
    void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options);

    /**
     * Read latest sync token from a connector instance.
     *
     * @param objectClass ConnId's object class.
     * @return latest sync token
     */
    SyncToken getLatestSyncToken(ObjectClass objectClass);

    /**
     * Get remote object.
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    ConnectorObject getObject(ObjectClass objectClass, Uid uid, OperationOptions options);

    /**
     * Search for remote objects.
     *
     * @param objectClass ConnId's object class
     * @param filter search filter
     * @param handler class responsible for working with the objects returned from the search; may be null.
     * @param options ConnId's OperationOptions
     */
    void search(
            ObjectClass objectClass,
            Filter filter,
            ResultsHandler handler,
            OperationOptions options);

    /**
     * Search for remote objects.
     *
     * @param objectClass ConnId's object class
     * @param filter search filter
     * @param handler class responsible for working with the objects returned from the search; may be null.
     * @param pageSize requested page results page size
     * @param pagedResultsCookie an opaque cookie which is used by the connector to track its position in the set of
     * query results
     * @param orderBy the sort keys which should be used for ordering the {@link ConnectorObject} returned by
     * search request
     * @param mapItems mapping items
     */
    void search(
            ObjectClass objectClass,
            Filter filter,
            ResultsHandler handler,
            int pageSize,
            String pagedResultsCookie,
            List<OrderByClause> orderBy,
            Iterator<? extends MappingItem> mapItems);

    /**
     * Builds metadata description of ConnId {@link ObjectClass}.
     *
     * @return metadata description of ConnId ObjectClass
     */
    Set<ObjectClassInfo> getObjectClassInfo();

    /**
     * Validate a connector instance.
     */
    void validate();

    /**
     * Check connection to resource.
     */
    void test();

    /**
     * Getter for active connector instance.
     *
     * @return active connector instance.
     */
    ConnInstance getConnInstance();
}
