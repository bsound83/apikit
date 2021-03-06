/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.metadata;

import org.mule.module.apikit.metadata.interfaces.ResourceLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class TestResourceLoader implements ResourceLoader {

  @Override
  public File getRamlResource(String relativePath) {
    try {
      URL resource = this.getClass().getResource(relativePath);
      if (resource == null)
        return null;
      return new File(resource.toURI());

    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    return null;
  }

}
