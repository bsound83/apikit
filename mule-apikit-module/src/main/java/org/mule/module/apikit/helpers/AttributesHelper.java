/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.helpers;

import org.mule.module.apikit.attributes.ApikitRequestAttributes;
import org.mule.module.apikit.HeaderNames;
import org.mule.runtime.http.api.domain.ParameterMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class AttributesHelper
{
  private AttributesHelper()
  {
      // Prevents instantiation :)
  }

  public static ParameterMap addParam(ParameterMap oldParams, String key, String value) {
    Map<String, LinkedList<String>> mapParam = new HashMap<>();
    LinkedList<String> valueList = new LinkedList<>();
    valueList.add(value);
    mapParam.put(key, valueList);
    for (Map.Entry<String, String> entry : oldParams.entrySet()) {
      LinkedList<String> list = new LinkedList<>();
      list.add(entry.getValue());
      mapParam.put(entry.getKey(), list);
    }
    return new ParameterMap(mapParam);
  }

  public static String addQueryString(String oldQueryString, int queryStringSize, String key, String value) {
    String newParam = queryStringSize != 0 ? "&" : "";
    try
    {
      newParam += URLEncoder.encode(key, "UTF-8");
      if (value != null)
      {

        newParam += "=" + URLEncoder.encode(value, "UTF-8");
      }
    }
    catch (UnsupportedEncodingException e)
    {
      //UTF-8 will never be unsupported
    }
    return oldQueryString + newParam;
  }

  public static ApikitRequestAttributes replaceParams(ApikitRequestAttributes attributes, ParameterMap headers, ParameterMap queryParams, String queryString, ParameterMap uriParams)
  {
    return new ApikitRequestAttributes(headers, attributes.getListenerPath(), attributes.getRelativePath(),
                                     attributes.getVersion(), attributes.getScheme(),
                                     attributes.getMethod(), attributes.getRequestPath(),
                                     attributes.getRequestUri(), queryString,
                                     queryParams, uriParams,
                                     attributes.getRemoteAddress(), attributes.getClientCertificate());
  }

  private static String ANY_RESPONSE_MEDIA_TYPE = "*/*";

  public static String getHeaderIgnoreCase(ApikitRequestAttributes attributes, String name)
  {
    ParameterMap headers = attributes.getHeaders();
    return getParamIgnoreCase(headers, name);
  }

  public static String getParamIgnoreCase(ParameterMap parameters, String name)
  {
    for (String header : parameters.keySet())
    {
      if (header.equalsIgnoreCase(name.toLowerCase()))
      {
        return parameters.get(header);
      }
    }
    return null;
  }

  public static String getMediaType(ApikitRequestAttributes attributes)
  {
    String contentType = getHeaderIgnoreCase(attributes, HeaderNames.CONTENT_TYPE);
    return contentType != null ? contentType.split(";")[0] : null;
  }

  public static String getAcceptedResponseMediaTypes(ParameterMap headers)
  {
    String acceptableResponseMediaTypes = getParamIgnoreCase(headers, "accept");
    if (acceptableResponseMediaTypes == null || acceptableResponseMediaTypes == "")
    {
      return ANY_RESPONSE_MEDIA_TYPE;
    }
    return acceptableResponseMediaTypes;
  }

}
