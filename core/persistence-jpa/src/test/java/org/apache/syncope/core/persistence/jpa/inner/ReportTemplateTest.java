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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ReportTemplateTest extends AbstractTest {

    @Autowired
    private ReportTemplateDAO reportTemplateDAO;

    @Test
    public void find() {
        ReportTemplate optin = reportTemplateDAO.find("sample");
        assertNotNull(optin);
        assertNotNull(optin.getFOTemplate());
        assertNotNull(optin.getCSVTemplate());
        assertNotNull(optin.getHTMLTemplate());
    }

    @Test
    public void findAll() {
        List<ReportTemplate> templates = reportTemplateDAO.findAll();
        assertNotNull(templates);
        assertFalse(templates.isEmpty());
    }

    @Test
    public void save() {
        ReportTemplate template = entityFactory.newEntity(ReportTemplate.class);
        template.setKey("new");
        template.setCSVTemplate(
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'></xsl:stylesheet>");

        ReportTemplate actual = reportTemplateDAO.save(template);
        assertNotNull(actual);
        assertNotNull(actual.getKey());
        assertNotNull(actual.getCSVTemplate());
        assertNull(actual.getHTMLTemplate());

        actual.setHTMLTemplate(
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'></xsl:stylesheet>");
        actual = reportTemplateDAO.save(actual);
        assertNotNull(actual.getCSVTemplate());
        assertNotNull(actual.getHTMLTemplate());
    }

    @Test
    public void delete() {
        reportTemplateDAO.delete("sample");
        assertNull(reportTemplateDAO.find("sample"));
    }
}
