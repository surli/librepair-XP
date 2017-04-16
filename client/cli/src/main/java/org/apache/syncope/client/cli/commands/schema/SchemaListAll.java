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
package org.apache.syncope.client.cli.commands.schema;

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaListAll extends AbstractSchemaCommand {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaListAll.class);

    private static final String LIST_HELP_MESSAGE = "schema --list-all";

    private final Input input;

    public SchemaListAll(final Input input) {
        this.input = input;
    }

    public void listAll() {
        if (input.parameterNumber() == 0) {
            try {
                for (final SchemaType schemaType : SchemaType.values()) {
                    schemaResultManager.toView(schemaType.name(), schemaSyncopeOperations.list(schemaType.name()));
                }
            } catch (final SyncopeClientException | WebServiceException ex) {
                LOG.error("Error listing schema", ex);
                schemaResultManager.genericError(ex.getMessage());
            }
        } else {
            schemaResultManager.unnecessaryParameters(input.listParameters(), LIST_HELP_MESSAGE);
        }
    }
}
