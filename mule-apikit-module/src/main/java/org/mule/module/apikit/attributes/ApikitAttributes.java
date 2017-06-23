/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.attributes;

import org.mule.runtime.core.message.BaseAttributes;
import org.mule.runtime.http.api.domain.ParameterMap;

/**
 * Base representation of HTTP message attributes.
 *
 * @since 1.0
 */
public abstract class ApikitAttributes extends BaseAttributes {

    /**
     * Map of HTTP headers in the message. Former properties.
     */
    protected ParameterMap headers;

    public ApikitAttributes(ParameterMap headers) {
        this.headers = headers.toImmutableParameterMap();
    }

    public ParameterMap getHeaders() {
        return headers;
    }
}
