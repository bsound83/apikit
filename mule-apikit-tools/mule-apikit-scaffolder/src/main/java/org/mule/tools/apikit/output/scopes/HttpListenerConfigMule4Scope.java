/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.output.scopes;

import static org.mule.tools.apikit.output.MuleConfigGenerator.HTTP_NAMESPACE;

import org.mule.tools.apikit.model.API;
import org.mule.tools.apikit.model.HttpListener4xConfig;

import org.jdom2.Element;


public class HttpListenerConfigMule4Scope implements Scope {

  private final Element mule;
  private final Element httpListenerConfig;

  public HttpListenerConfigMule4Scope(API api, Element mule) {
    this.mule = mule;

    if (api.getHttpListenerConfig() != null) {
      httpListenerConfig = new Element(HttpListener4xConfig.ELEMENT_NAME, HTTP_NAMESPACE.getNamespace());
      httpListenerConfig.setAttribute("name", api.getHttpListenerConfig().getName());
      String basePath = api.getHttpListenerConfig().getBasePath();
      if (basePath != null && basePath != "/" && basePath != "") {
        httpListenerConfig.setAttribute("basePath", api.getHttpListenerConfig().getBasePath());
      }
      mule.addContent(httpListenerConfig);
      Element connection = new Element("listener-connection", HTTP_NAMESPACE.getNamespace());
      connection.setAttribute("host", api.getHttpListenerConfig().getHost());
      connection.setAttribute("port", api.getHttpListenerConfig().getPort());
      httpListenerConfig.addContent(connection);
    } else
      httpListenerConfig = null;
  }

  @Override
  public Element generate() {
    return httpListenerConfig;

  }
}
