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
package org.apache.syncope.client.console.wicket.ajax;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.util.time.Duration;

/**
 * An {@link AbstractAjaxTimerBehavior} not showing veil.
 */
public abstract class IndicatorAjaxTimerBehavior extends AbstractAjaxTimerBehavior implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 8863750325559215077L;

    public IndicatorAjaxTimerBehavior(final Duration updateInterval) {
        super(updateInterval);
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return StringUtils.EMPTY;
    }
}
